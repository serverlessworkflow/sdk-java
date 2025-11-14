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

import static io.serverlessworkflow.fluent.spec.dsl.DSL.call;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.emit;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.event;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.http;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.set;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.tryCatch;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.types.Errors;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class TryCatchDslTest {

  private static final String EXPR_ENDPOINT = "${ \"https://api.example.com/v1/resource\" }";

  @Test
  void when_try_with_tasks_and_catch_when_with_retry_and_tasks() {
    Workflow wf =
        WorkflowBuilder.workflow("f", "ns", "1")
            .tasks(
                t ->
                    t.tryCatch(
                        tryCatch()
                            // try block (one HTTP call)
                            .tasks(call(http().GET().endpoint(EXPR_ENDPOINT)))
                            // catch block
                            .catches()
                            .when("$.error == true")
                            .errors(Errors.RUNTIME, 500)
                            .tasks(emit(e -> e.event(event().type("org.acme.failed"))))
                            .retry()
                            .when("$.retries < 3")
                            .limit("PT5S")
                            .jitter("PT0.1S", "PT0.5S")
                            .done() // back to TryCatchSpec
                            .done() // back to TrySpec
                        ))
            .build();

    // --- locate Try task ---
    var tryTask = wf.getDo().get(0).getTask().getTryTask();
    assertThat(tryTask).isNotNull();

    // --- assert try/do tasks ---
    var tryDo = tryTask.getTry();
    assertThat(tryDo).isNotNull();
    assertThat(tryDo).hasSize(1);

    // First inner task: Call HTTP GET to EXPR_ENDPOINT
    var callArgs = tryDo.get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(callArgs.getMethod()).isEqualTo("GET");
    assertThat(callArgs.getEndpoint().getRuntimeExpression()).isEqualTo(EXPR_ENDPOINT);

    // --- assert catch (singular) ---
    var cat = tryTask.getCatch();
    assertThat(cat).isNotNull();

    // when condition
    assertThat(cat.getWhen()).isEqualTo("$.error == true");

    // errors filter (Errors.RUNTIME + 500)
    var errors = cat.getErrors();
    assertThat(errors).isNotNull();
    assertThat(errors.getWith().getType()).isEqualTo(Errors.RUNTIME.toString());
    assertThat(errors.getWith().getStatus()).isEqualTo(500);

    // catch/do tasks
    var catchDo = cat.getDo();
    assertThat(catchDo).isNotNull();
    assertThat(catchDo).hasSize(1);
    var emitted = catchDo.get(0).getTask().getEmitTask().getEmit().getEvent().getWith();
    assertThat(emitted.getType()).isEqualTo("org.acme.failed");

    // retry policy (attached to this catch)
    var retry = cat.getRetry().getRetryPolicyDefinition();
    assertThat(retry).isNotNull();
    assertThat(retry.getWhen()).isEqualTo("$.retries < 3");
    assertThat(retry.getLimit().getDuration().getDurationExpression()).isEqualTo("PT5S");

    // jitter range if modeled with from/to
    if (retry.getJitter() != null) {
      assertThat(retry.getJitter().getFrom().getDurationExpression()).isEqualTo("PT0.1S");
      assertThat(retry.getJitter().getTo().getDurationExpression()).isEqualTo("PT0.5S");
    }
  }

  @Test
  void when_try_with_multiple_tasks_and_catch_except_when_with_uri_error_filter() throws Exception {
    URI errType = new URI("urn:example:error:downstream");

    Workflow wf =
        WorkflowBuilder.workflow("g", "ns", "1")
            .tasks(
                t ->
                    t.tryCatch(
                        tryCatch()
                            // try with two tasks
                            .tasks(
                                call(http().GET().endpoint(EXPR_ENDPOINT)),
                                set("$.status = \"IN_FLIGHT\""))
                            // catch with exceptWhen + explicit URI error filter + status
                            .catches()
                            .exceptWhen("$.code == 502")
                            .errors(errType, 502)
                            .tasks(emit(e -> e.event(event().type("org.acme.recover"))))
                            .done() // back to TrySpec
                        ))
            .build();

    var tryTask = wf.getDo().get(0).getTask().getTryTask();
    assertThat(tryTask).isNotNull();

    // try has two tasks
    var tryDo = tryTask.getTry();
    assertThat(tryDo).isNotNull();
    assertThat(tryDo).hasSize(2);

    // First is HTTP call (GET + endpoint)
    var args0 = tryDo.get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args0.getMethod()).isEqualTo("GET");
    assertThat(args0.getEndpoint().getRuntimeExpression()).isEqualTo(EXPR_ENDPOINT);

    // Second is set (presence check)
    var setTask = tryDo.get(1).getTask().getSetTask();
    assertThat(setTask).isNotNull();

    // catch (singular)
    var cat = tryTask.getCatch();
    assertThat(cat).isNotNull();
    assertThat(cat.getExceptWhen()).isEqualTo("$.code == 502");

    var errors = cat.getErrors();
    assertThat(errors).isNotNull();
    assertThat(errors.getWith().getType()).isEqualTo(errType.toString());
    assertThat(errors.getWith().getStatus()).isEqualTo(502);

    var catchDo = cat.getDo();
    assertThat(catchDo).hasSize(1);
    var ev = catchDo.get(0).getTask().getEmitTask().getEmit().getEvent().getWith();
    assertThat(ev.getType()).isEqualTo("org.acme.recover");
  }

  @Test
  void when_try_with_catch_and_simple_retry_limit_only() {
    Workflow wf =
        WorkflowBuilder.workflow("r", "ns", "1")
            .tasks(
                t ->
                    t.tryCatch(
                        tryCatch()
                            .tasks(call(http().GET().endpoint(EXPR_ENDPOINT)))
                            .catches()
                            .when("$.fail == true")
                            .errors(Errors.COMMUNICATION, 503)
                            .retry()
                            .limit("PT2S")
                            .done()
                            .tasks(emit(e -> e.event(event().type("org.acme.retrying"))))
                            .done()))
            .build();

    var tryTask = wf.getDo().get(0).getTask().getTryTask();
    assertThat(tryTask).isNotNull();

    var tryDo = tryTask.getTry();
    assertThat(tryDo).isNotNull();
    assertThat(tryDo).hasSize(1);
    assertThat(tryDo.get(0).getTask().getCallTask()).isNotNull();

    var cat = tryTask.getCatch();
    assertThat(cat).isNotNull();
    assertThat(cat.getWhen()).isEqualTo("$.fail == true");

    var err = cat.getErrors().getWith();
    assertThat(err.getType()).isEqualTo(Errors.COMMUNICATION.toString());
    assertThat(err.getStatus()).isEqualTo(503);

    var retryDef = cat.getRetry().getRetryPolicyDefinition();
    assertThat(retryDef).isNotNull();
    assertThat(retryDef.getLimit().getDuration().getDurationExpression()).isEqualTo("PT2S");
    // 'when' not set in this case
    assertThat(retryDef.getWhen()).isNull();

    var catchDo = cat.getDo();
    assertThat(catchDo).hasSize(1);
    var ev = catchDo.get(0).getTask().getEmitTask().getEmit().getEvent().getWith();
    assertThat(ev.getType()).isEqualTo("org.acme.retrying");
  }
}
