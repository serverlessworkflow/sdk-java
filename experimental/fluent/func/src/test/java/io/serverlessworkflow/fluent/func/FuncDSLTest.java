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
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.call;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emit;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.event;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.get;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.http;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.auth;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.use;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.cloudevents.core.data.BytesCloudEventData;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import io.serverlessworkflow.fluent.func.dsl.FuncEmitSpec;
import io.serverlessworkflow.fluent.func.dsl.FuncListenSpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for Step chaining (exportAs/when) over function/emit/listen. */
class FuncDSLTest {

  @Test
  void function_step_exportAs_function_sets_export() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("step-function-export")
            .tasks(
                // call + chain exportAs
                function(String::trim, String.class)
                    .exportAs((String s) -> Map.of("len", s.length())))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");
    Export ex = ((CallJava) t.getCallTask().get()).getExport();
    assertNotNull(ex, "Export should be set via Step.exportAs(Function)");
    assertNotNull(ex.getAs(), "'as' should be populated");
    // functional export should not produce a literal string
    assertNull(
        ex.getAs().getString(), "Export 'as' must not be a literal string when using Function");
  }

  @Test
  void function_step_when_compiles_and_builds() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("step-function-when")
            .tasks(
                function((Integer v) -> v + 1, Integer.class)
                    .when((Integer v) -> v > 0, Integer.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    assertNotNull(items.get(0).getTask().getCallTask(), "CallTask should still be present");
    // We don't assert internal predicate storage details; just ensure build success & presence.
  }

  @Test
  void emit_step_exportAs_javaFilter_sets_export() {
    // Build an emit spec using your DSL (type + data function)
    FuncEmitSpec spec =
        new FuncEmitSpec()
            .type("org.acme.signal")
            .bytesData((String s) -> s.getBytes(StandardCharsets.UTF_8), String.class);

    // JavaFilterFunction<T,R> is (T, WorkflowContextData, TaskContextData) -> R
    JavaFilterFunction<String, Map<String, Object>> jf =
        (val, wfCtx, taskCtx) -> Map.of("wrapped", val, "wfId", wfCtx.instanceData().id());

    Workflow wf =
        FuncWorkflowBuilder.workflow("step-emit-export")
            .tasks(emit("emitWrapped", spec).exportAs(jf)) // chaining on Step
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    Task t = items.get(0).getTask();
    assertNotNull(t.getEmitTask(), "EmitTask expected");

    // Export is attached to Task
    Export ex = t.getEmitTask().getExport();
    assertNotNull(ex, "Export should be set via Step.exportAs(JavaFilterFunction)");
    assertNotNull(ex.getAs(), "'as' should be populated");
    assertNull(
        ex.getAs().getString(), "Export 'as' must not be a literal string when using function");
  }

  @Test
  @DisplayName("listen(spec).exportAs(Function) sets Export on ListenTask holder")
  void listen_step_exportAs_function_sets_export() {
    FuncListenSpec spec = toOne("org.acme.review.done"); // using your existing DSL helper

    Workflow wf =
        FuncWorkflowBuilder.workflow("step-listen-export")
            .tasks(listen("waitHumanReview", spec).exportAs((Object e) -> Map.of("seen", true)))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected");

    Export ex = t.getListenTask().getExport();
    assertNotNull(ex, "Export should be set via Step.exportAs(Function)");
    assertNotNull(ex.getAs(), "'as' should be populated");
    assertNull(
        ex.getAs().getString(), "Export 'as' must not be a literal string when using function");
  }

  @Test
  @DisplayName("emit(event(type, fn)).when(...) -> still an EmitTask and builds")
  void emit_step_when_compiles_and_builds() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("step-emit-when")
            .tasks(
                emit(event("org.acme.sig", (String s) -> BytesCloudEventData.wrap(s.getBytes())))
                    .when((Object ctx) -> true))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    assertNotNull(items.get(0).getTask().getEmitTask(), "EmitTask should still be present");
  }

  @Test
  @DisplayName("Mixed chaining: function.exportAs -> emit.when -> listen.exportAs")
  void mixed_chaining_order_and_exports() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("step-mixed")
            .tasks(
                function(String::strip, String.class).exportAs((String s) -> Map.of("s", s)),
                emit(event(
                        "org.acme.kickoff", (String s) -> BytesCloudEventData.wrap(s.getBytes())))
                    .when((Object ignore) -> true),
                listen(toOne("org.acme.done")).exportAs((Object e) -> Map.of("ok", true)))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(3, items.size());

    Task t0 = items.get(0).getTask();
    Task t1 = items.get(1).getTask();
    Task t2 = items.get(2).getTask();

    assertNotNull(t0.getCallTask());
    assertNotNull(t1.getEmitTask());
    assertNotNull(t2.getListenTask());

    assertNotNull(
        ((CallJava) t0.getCallTask().get()).getExport(), "function step should carry export");
    assertNotNull(t2.getListenTask().getExport(), "listen step should carry export");
  }

  @Test
  void switchWhenOrElse_jq_to_taskName() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("jqSwitch")
            .tasks(FuncDSL.switchWhenOrElse(".approved", "send", "draft"))
            .build();
    Task switchTask = wf.getDo().get(0).getTask();
    assertNotNull(switchTask.getSwitchTask());
    var items = switchTask.getSwitchTask().getSwitch();
    assertEquals(2, items.size());
    assertEquals(".approved", items.get(0).getSwitchCase().getWhen());
  }

  @Test
  void switchWhenOrElse_jq_to_directive() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("jqSwitchDir")
            .tasks(FuncDSL.switchWhenOrElse(".score >= 80", "pass", FlowDirectiveEnum.END))
            .build();
    Task switchTask = wf.getDo().get(0).getTask();
    var items = switchTask.getSwitchTask().getSwitch();
    assertEquals(".score >= 80", items.get(0).getSwitchCase().getWhen());
    assertEquals(
        FlowDirectiveEnum.END, items.get(1).getSwitchCase().getThen().getFlowDirectiveEnum());
  }

  @Test
  void http_spec_via_call_builds_call_http_task() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("http-call-spec")
            .tasks(call("checkHealth", http().GET().endpoint("http://service/health")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected for HTTP call");

    // HTTP-specific call
    assertInstanceOf(
        CallHTTP.class, t.getCallTask().get(), "CallTask should be an instance of CallHTTP");

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("GET", http.getWith().getMethod(), "HTTP method should be GET");
    assertEquals(
        "http://service/health",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString(),
        "endpoint should match the DSL endpoint");
  }

  @Test
  @DisplayName("get(endpoint) convenience creates unnamed GET CallHTTP task")
  void get_convenience_creates_http_get() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("http-get-convenience")
            .tasks(get("http://service/status"))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("GET", http.getWith().getMethod());
    assertEquals(
        "http://service/status",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString(),
        "endpoint should be set from get(endpoint)");
  }

  @Test
  @DisplayName("get(name, endpoint, auth -> auth.use(\"auth-id\")) wires authentication")
  void get_named_with_authentication_uses_auth_policy() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("http-get-auth")
            .tasks(get("fetchUsers", "http://service/api/users", use("user-service-auth")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    assertEquals("fetchUsers", items.get(0).getName(), "Task should use the provided name");
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("GET", http.getWith().getMethod());
    assertEquals(
        "http://service/api/users",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString(),
        "endpoint should be set from get(name, endpoint, auth)");

    assertNotNull(
        http.getWith().getEndpoint().getEndpointConfiguration().getAuthentication(),
        "authentication should be configured");
    assertEquals(
        "user-service-auth",
        http.getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicyReference()
            .getUse(),
        "auth.use(\"...\") should set authentication.use");
  }

  @Test
  @DisplayName("get(URI endpoint, auth) uses URI and authentication")
  void get_with_uri_and_authentication() {
    URI endpoint = URI.create("https://service.example.com/api/health");

    Workflow wf =
        FuncWorkflowBuilder.workflow("http-get-uri-auth")
            .tasks(get("checkHealth", endpoint, auth -> auth.use("tls-auth")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertEquals("checkHealth", items.get(0).getName(), "Task should use the provided name");
    assertNotNull(t.getCallTask());

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("GET", http.getWith().getMethod());
    assertEquals(
        endpoint.toString(),
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString(),
        "endpoint should be derived from URI");

    assertNotNull(http.getWith().getEndpoint().getEndpointConfiguration().getAuthentication());
    assertEquals(
        "tls-auth",
        http.getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicyReference()
            .getUse());
  }

  @Test
  @DisplayName("post(body, endpoint) convenience creates POST CallHTTP with body")
  void post_convenience_creates_http_post_with_body() {
    Map<String, Object> body = Map.of("name", "Ricardo");

    Workflow wf =
        FuncWorkflowBuilder.workflow("http-post-convenience")
            .tasks(FuncDSL.post(body, "http://service/api/users"))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("POST", http.getWith().getMethod());
    assertEquals(
        "http://service/api/users",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString(),
        "endpoint should be set from post(body, endpoint)");

    assertNotNull(http.getWith().getBody(), "Body should be set on POST");
    assertEquals(body, http.getWith().getBody(), "Body should match the provided payload");
  }

  @Test
  @DisplayName("post(name, body, endpoint, auth) wires name, method, endpoint, body and auth")
  void post_named_with_authentication() {
    Map<String, Object> body = Map.of("id", 123, "status", "NEW");

    Workflow wf =
        FuncWorkflowBuilder.workflow("http-post-named-auth")
            .tasks(
                FuncDSL.post(
                    "createOrder",
                    body,
                    "https://orders.example.com/api/orders",
                    auth -> auth.use("orders-auth")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertEquals("createOrder", items.get(0).getName(), "Task should use the provided name");
    assertNotNull(t.getCallTask());

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("POST", http.getWith().getMethod());
    assertEquals(
        "https://orders.example.com/api/orders",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString());
    assertEquals(body, http.getWith().getBody());

    assertNotNull(http.getWith().getEndpoint().getEndpointConfiguration().getAuthentication());
    assertEquals(
        "orders-auth",
        http.getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicyReference()
            .getUse());
  }

  @Test
  @DisplayName("call(http(\"...\", auth)) reuses fluent HTTP spec in call(...)")
  void call_with_preconfigured_http_spec() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("http-call-preconfigured")
            .tasks(
                call(
                    "preconfigured",
                    http("http://service/api", auth -> auth.use("svc-auth"))
                        .POST()
                        .body(Map.of("foo", "bar"))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertEquals("preconfigured", items.get(0).getName());
    assertNotNull(t.getCallTask());

    CallHTTP http = (CallHTTP) t.getCallTask().get();
    assertEquals("POST", http.getWith().getMethod());
    assertEquals(
        "http://service/api",
        http.getWith().getEndpoint().getUriTemplate().getLiteralUri().toString());
    assertEquals(
        "svc-auth",
        http.getWith()
            .getEndpoint()
            .getEndpointConfiguration()
            .getAuthentication()
            .getAuthenticationPolicyReference()
            .getUse());
    assertEquals(Map.of("foo", "bar"), http.getWith().getBody());
  }
}
