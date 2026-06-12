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
import static io.serverlessworkflow.fluent.spec.dsl.DSL.http;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CallHttpDslExtensionsTest {

  @Test
  void when_put_method_shortcut() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().PUT().uri("http://test.com")))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getWith().getMethod()).isEqualTo("PUT");
  }

  @Test
  void when_delete_method_shortcut() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().DELETE().uri("http://test.com")))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getWith().getMethod()).isEqualTo("DELETE");
  }

  @Test
  void when_patch_method_shortcut() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().PATCH().uri("http://test.com")))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getWith().getMethod()).isEqualTo("PATCH");
  }

  @Test
  void when_head_method_shortcut() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().HEAD().uri("http://test.com")))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getWith().getMethod()).isEqualTo("HEAD");
  }

  @Test
  void when_options_method_shortcut() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().OPTIONS().uri("http://test.com")))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getWith().getMethod()).isEqualTo("OPTIONS");
  }

  @Test
  void when_redirect_true() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().redirect(true).POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.isRedirect()).isTrue();
  }

  @Test
  void when_redirect_false() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().redirect(false).POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.isRedirect()).isFalse();
  }

  @Test
  void when_acceptXML() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().acceptXML().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Accept"))
        .isEqualTo("application/xml");
  }

  @Test
  void when_acceptForm() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().acceptForm().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Accept"))
        .isEqualTo("application/x-www-form-urlencoded");
  }

  @Test
  void when_acceptText() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().acceptText().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Accept"))
        .isEqualTo("text/plain");
  }

  @Test
  void when_contentTypeXML() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().contentTypeXML().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Content-Type"))
        .isEqualTo("application/xml");
  }

  @Test
  void when_contentTypeForm() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().contentTypeForm().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Content-Type"))
        .isEqualTo("application/x-www-form-urlencoded");
  }

  @Test
  void when_contentTypeText() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(call(http().contentTypeText().POST().uri("http://test.com")))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Content-Type"))
        .isEqualTo("text/plain");
  }

  @Test
  void when_andThen_adds_query() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(
                call(
                    http()
                        .GET()
                        .uri("http://test.com")
                        .andThen(b -> b.query(Map.of("key", "value")))))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getQuery().getHTTPQuery().getAdditionalProperties().get("key"))
        .isEqualTo("value");
  }

  @Test
  void when_andThen_adds_body() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(
                call(
                    http()
                        .POST()
                        .uri("http://test.com")
                        .andThen(b -> b.body(Map.of("foo", "bar")))))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.getBody()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) args.getBody();
    assertThat(body).containsEntry("foo", "bar");
  }

  @Test
  void when_andThen_chained_multiple_times() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(
                call(
                    http()
                        .GET()
                        .uri("http://test.com")
                        .andThen(b -> b.redirect(false))
                        .andThen(b -> b.query(Map.of("q", "1")))
                        .andThen(b -> b.body(Map.of("key", "value")))))
            .build();
    HTTPArguments args = wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith();
    assertThat(args.isRedirect()).isFalse();
    assertThat(args.getQuery().getHTTPQuery().getAdditionalProperties().get("q")).isEqualTo("1");
    assertThat(args.getBody()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) args.getBody();
    assertThat(body).containsEntry("key", "value");
  }

  @Test
  void when_outputAs_with_expression() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(
                call(http().POST().uri("http://test.com").andThen(b -> b.outputAs("$.firstName"))))
            .build();
    // output.as is set on the CallHTTP task itself
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertThat(call.getOutput().getAs().getString()).isEqualTo("$.firstName");
  }

  @Test
  void when_http_call_with_all_features() {
    var wf =
        WorkflowBuilder.workflow("test", "ns", "1")
            .tasks(
                call(
                    "myTask",
                    http()
                        .contentTypeJSON()
                        .acceptJSON()
                        .redirect(true)
                        .POST()
                        .uri("http://localhost:9876/api/v1/authors")
                        .body(Map.of("firstName", "John", "lastName", "Doe"))
                        .query(Map.of("sort", "asc"))
                        .header("X-Custom", "value")
                        .andThen(b -> b.outputAs("$.id"))))
            .build();

    var taskItem = wf.getDo().get(0);
    assertThat(taskItem.getName()).isEqualTo("myTask");

    CallHTTP call = taskItem.getTask().getCallTask().getCallHTTP();
    HTTPArguments args = call.getWith();

    assertThat(args.getMethod()).isEqualTo("POST");
    assertThat(args.isRedirect()).isTrue();
    assertThat(args.getBody()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) args.getBody();
    assertThat(body).containsEntry("firstName", "John").containsEntry("lastName", "Doe");
    assertThat(args.getQuery().getHTTPQuery().getAdditionalProperties().get("sort"))
        .isEqualTo("asc");
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Content-Type"))
        .isEqualTo("application/json");
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("Accept"))
        .isEqualTo("application/json");
    assertThat(args.getHeaders().getHTTPHeaders().getAdditionalProperties().get("X-Custom"))
        .isEqualTo("value");

    assertThat(call.getOutput().getAs().getString()).isEqualTo("$.id");
  }
}
