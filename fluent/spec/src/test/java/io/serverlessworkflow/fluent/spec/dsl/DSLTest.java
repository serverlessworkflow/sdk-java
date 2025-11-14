/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.fluent.spec.dsl;

import static io.serverlessworkflow.fluent.spec.dsl.DSL.auth;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.basic;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.bearer;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.call;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.error;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.event;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.http;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.secrets;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.to;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.types.Errors;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class DSLTest {

  @Test
  public void when_new_http_call_task() {
    Workflow wf =
        WorkflowBuilder.workflow("myFlow", "myNs", "1.2.3")
            .tasks(
                call(
                    http()
                        .acceptJSON()
                        .header("CustomKey", "CustomValue")
                        .POST()
                        .endpoint("${ \"https://petstore.swagger.io/v2/pet/\\(.petId)\" }")))
            .build();

    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args).isNotNull();
    assertThat(args.getMethod()).isEqualTo("POST");
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Accept"))
        .isEqualTo("application/json");
    assertThat(args.getEndpoint().getRuntimeExpression())
        .isEqualTo("${ \"https://petstore.swagger.io/v2/pet/\\(.petId)\" }");
  }

  @Test
  public void when_listen_all_then_emit() {
    Workflow wf =
        WorkflowBuilder.workflow("myFlow", "myNs", "1.2.3")
            .tasks(
                t ->
                    t.listen(
                            to().all(
                                    event().type("org.acme.listen"),
                                    event().type("org.example.listen")))
                        .emit(e -> e.event(event().type("org.example.emit"))))
            .build();

    // Sanity
    assertThat(wf).isNotNull();
    assertThat(wf.getDo()).hasSize(2);

    // --- First task: LISTEN with ALL ---
    var first = wf.getDo().get(0).getTask();
    assertThat(first.getListenTask()).isNotNull();

    var listen = first.getListenTask().getListen();
    assertThat(listen).isNotNull();
    var to = listen.getTo();
    assertThat(to).isNotNull();

    // Exclusivity: ALL set; ONE/ANY not set
    assertThat(to.getAllEventConsumptionStrategy()).isNotNull();
    assertThat(to.getOneEventConsumptionStrategy()).isNull();
    assertThat(to.getAnyEventConsumptionStrategy()).isNull();

    // ALL has exactly 2 EventFilters, in insertion order
    var all = to.getAllEventConsumptionStrategy().getAll();
    assertThat(all).hasSize(2);
    assertThat(all.get(0).getWith()).isNotNull();
    assertThat(all.get(1).getWith()).isNotNull();
    assertThat(all.get(0).getWith().getType()).isEqualTo("org.acme.listen");
    assertThat(all.get(1).getWith().getType()).isEqualTo("org.example.listen");

    // --- Second task: EMIT ---
    var second = wf.getDo().get(1).getTask();
    assertThat(second.getEmitTask()).isNotNull();

    var emit = second.getEmitTask().getEmit();
    assertThat(emit).isNotNull();
    // Event type set; id/time/contentType are unset unless explicitly configured
    var evProps = emit.getEvent().getWith();
    assertThat(evProps.getType()).isEqualTo("org.example.emit");
    assertThat(evProps.getId()).isNull();
    assertThat(evProps.getTime()).isNull();
    assertThat(evProps.getDatacontenttype()).isNull();
  }

  @Test
  public void when_listen_any_with_until() {
    final String untilExpr = "$.count > 0";

    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(
                t ->
                    t.listen(
                        to().any(event().type("A"), event().type("B"))
                            .until(untilExpr)
                            .andThen(c -> c.read(ListenTaskConfiguration.ListenAndReadAs.RAW))))
            .build();

    var to = wf.getDo().get(0).getTask().getListenTask().getListen().getTo();

    assertThat(to.getAnyEventConsumptionStrategy()).isNotNull();
    assertThat(to.getOneEventConsumptionStrategy()).isNull();
    assertThat(to.getAllEventConsumptionStrategy()).isNull();

    assertThat(wf.getDo().get(0).getTask().getListenTask().getListen().getRead())
        .isEqualTo(ListenTaskConfiguration.ListenAndReadAs.RAW);

    var any = to.getAnyEventConsumptionStrategy();
    assertThat(any.getAny()).hasSize(2);
    assertThat(any.getAny().get(0).getWith().getType()).isEqualTo("A");
    assertThat(any.getAny().get(1).getWith().getType()).isEqualTo("B");
    assertThat(any.getUntil().getAnyEventUntilCondition()).isEqualTo(untilExpr);
  }

  @Test
  public void when_listen_one() {
    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(t -> t.listen(to().one(event().type("only-once"))))
            .build();

    var to = wf.getDo().get(0).getTask().getListenTask().getListen().getTo();

    assertThat(to.getOneEventConsumptionStrategy()).isNotNull();
    assertThat(to.getAllEventConsumptionStrategy()).isNull();
    assertThat(to.getAnyEventConsumptionStrategy()).isNull();

    var one = to.getOneEventConsumptionStrategy().getOne();
    assertThat(one.getWith()).isNotNull();
    assertThat(one.getWith().getType()).isEqualTo("only-once");
  }

  @Test
  public void when_raise_with_string_type_and_status() {
    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(
                t ->
                    t.raise(
                        error("org.acme.Error", 422).title("Unprocessable").detail("Bad input")))
            .build();

    var err = wf.getDo().get(0).getTask().getRaiseTask().getRaise().getError();

    assertThat(err).isNotNull();
    assertThat(err.getRaiseErrorDefinition()).isNotNull();
    assertThat(err.getRaiseErrorDefinition().getType().getExpressionErrorType())
        .isEqualTo("org.acme.Error");
    assertThat(err.getRaiseErrorDefinition().getStatus()).isEqualTo(422);
    assertThat(err.getRaiseErrorDefinition().getTitle().getExpressionErrorTitle())
        .isEqualTo("Unprocessable");
    assertThat(err.getRaiseErrorDefinition().getDetail().getExpressionErrorDetails())
        .isEqualTo("Bad input");
  }

  @Test
  public void when_raise_with_string_type_only() {
    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(t -> t.raise(error("org.acme.MinorError")))
            .build();

    var err = wf.getDo().get(0).getTask().getRaiseTask().getRaise().getError();

    assertThat(err).isNotNull();
    assertThat(err.getRaiseErrorDefinition()).isNotNull();
    // type as expression
    assertThat(err.getRaiseErrorDefinition().getType().getExpressionErrorType())
        .isEqualTo("org.acme.MinorError");
    // status/title/detail not set
    assertThat(err.getRaiseErrorDefinition().getStatus()).isEqualTo(0);
    assertThat(err.getRaiseErrorDefinition().getTitle()).isNull();
    assertThat(err.getRaiseErrorDefinition().getDetail()).isNull();
  }

  @Test
  public void when_raise_with_uri_type_and_status() throws Exception {
    URI type = new URI("urn:example:error:bad-request");

    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(t -> t.raise(error(type, 400).title("Bad Request").detail("Missing field")))
            .build();

    var err = wf.getDo().get(0).getTask().getRaiseTask().getRaise().getError();

    assertThat(err).isNotNull();
    assertThat(err.getRaiseErrorDefinition()).isNotNull();
    // type as URI
    assertThat(err.getRaiseErrorDefinition().getType().getLiteralErrorType().getLiteralUri())
        .isEqualTo(type);
    assertThat(err.getRaiseErrorDefinition().getStatus()).isEqualTo(400);
    assertThat(err.getRaiseErrorDefinition().getTitle().getExpressionErrorTitle())
        .isEqualTo("Bad Request");
    assertThat(err.getRaiseErrorDefinition().getDetail().getExpressionErrorDetails())
        .isEqualTo("Missing field");
  }

  @Test
  public void when_raise_with_uri_type_only() throws Exception {
    URI type = new URI("urn:example:error:temporary");

    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(t -> t.raise(error(type).title("Temporary")))
            .build();

    var err = wf.getDo().get(0).getTask().getRaiseTask().getRaise().getError();

    assertThat(err).isNotNull();
    assertThat(err.getRaiseErrorDefinition()).isNotNull();
    // type as URI
    assertThat(err.getRaiseErrorDefinition().getType().getLiteralErrorType().getLiteralUri())
        .isEqualTo(type);
    // status not set
    assertThat(err.getRaiseErrorDefinition().getStatus()).isEqualTo(0);
    // title set, detail not set
    assertThat(err.getRaiseErrorDefinition().getTitle().getExpressionErrorTitle())
        .isEqualTo("Temporary");
    assertThat(err.getRaiseErrorDefinition().getDetail()).isNull();
  }

  @Test
  void serverError_uses_versioned_uri_and_default_code() {
    Errors.setSpecVersion("1.1.0"); // flip version globally
    var spec = DSL.serverError().title("Boom").detail("x");

    var wf = WorkflowBuilder.workflow("f", "ns", "1").tasks(t -> t.raise(spec)).build();
    var def =
        wf.getDo().get(0).getTask().getRaiseTask().getRaise().getError().getRaiseErrorDefinition();

    assertThat(def.getType().getLiteralErrorType().getLiteralUri().toString())
        .isEqualTo("https://serverlessworkflow.io/spec/1.1.0/errors/runtime");
    assertThat(def.getStatus()).isEqualTo(500);
    assertThat(def.getTitle().getExpressionErrorTitle()).isEqualTo("Boom");
    assertThat(def.getDetail().getExpressionErrorDetails()).isEqualTo("x");
  }

  @Test
  void use_spec_accumulates_secrets_and_auths() {
    Workflow wf =
        WorkflowBuilder.workflow("id", "ns", "1")
            .use(
                auth("basic-auth", basic("u", "p")),
                auth("bearer-auth", bearer("t")),
                secrets("s1", "s2"))
            .build();

    var use = wf.getUse();
    assertThat(use).isNotNull();
    assertThat(use.getSecrets()).containsExactly("s1", "s2");
    assertThat(use.getAuthentications().getAdditionalProperties().keySet())
        .containsExactly("basic-auth", "bearer-auth");
  }
}
