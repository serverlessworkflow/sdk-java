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
package io.serverlessworkflow.fluent.spec;

import static io.serverlessworkflow.fluent.spec.WorkflowBuilderConsumers.authBasic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CatchErrors;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.ErrorFilter;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.HTTPHeaders;
import io.serverlessworkflow.api.types.HTTPQuery;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.OneEventConsumptionStrategy;
import io.serverlessworkflow.api.types.RetryLimitAttempt;
import io.serverlessworkflow.api.types.RetryPolicy;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.api.types.Use;
import io.serverlessworkflow.api.types.UseAuthentications;
import io.serverlessworkflow.api.types.Workflow;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for the fluent WorkflowBuilder API (using static consumers). */
public class WorkflowBuilderTest {

  @Test
  void testWorkflowDocumentDefaults() {
    // Use default name, namespace, version
    Workflow wf = WorkflowBuilder.workflow().build();
    assertNotNull(wf, "Workflow should not be null");
    Document doc = wf.getDocument();
    assertNotNull(doc, "Document should not be null");
    assertEquals("org.acme", doc.getNamespace(), "Default namespace should be org.acme");
    assertEquals("0.0.1", doc.getVersion(), "Default version should be 0.0.1");
    assertEquals("1.0.0", doc.getDsl(), "DSL version should be set to 1.0.0");
    assertNotNull(doc.getName(), "Name should be auto-generated");
  }

  @Test
  void testWorkflowDocumentExplicit() {
    Workflow wf =
        WorkflowBuilder.workflow("myFlow", "myNs", "1.2.3")
            .document(d -> d.dsl("1.0.0").namespace("myNs").name("myFlow").version("1.2.3"))
            .build();

    Document doc = wf.getDocument();
    assertEquals("1.0.0", doc.getDsl());
    assertEquals("myNs", doc.getNamespace());
    assertEquals("myFlow", doc.getName());
    assertEquals("1.2.3", doc.getVersion());
  }

  @Test
  void testUseAuthenticationsBasic() {
    Workflow wf =
        WorkflowBuilder.workflow("flowAuth")
            .use(
                u ->
                    u.authentications(
                        a -> a.authentication("basicAuth", authBasic("admin", "pass"))))
            .build();

    Use use = wf.getUse();
    assertNotNull(use, "Use must not be null");
    UseAuthentications auths = use.getAuthentications();
    assertNotNull(auths, "Authentications map must not be null");
    AuthenticationPolicyUnion union = auths.getAdditionalProperties().get("basicAuth");
    assertNotNull(union, "basicAuth policy should be present");
    assertNotNull(union.getBasicAuthenticationPolicy(), "BasicAuthenticationPolicy should be set");
  }

  @Test
  void testDoTaskSetAndForEach() {
    Workflow wf =
        WorkflowBuilder.workflow("flowDo")
            .tasks(
                d ->
                    d.set("initCtx", "$.foo = 'bar'")
                        .forEach("item", f -> f.each("item").at("$.list")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertNotNull(items, "Do list must not be null");
    assertEquals(2, items.size(), "There should be two tasks");

    TaskItem setItem = items.get(0);
    assertEquals("initCtx", setItem.getName());
    SetTask st = setItem.getTask().getSetTask();
    assertNotNull(st, "SetTask should be present");
    assertEquals("$.foo = 'bar'", st.getSet().getString());

    TaskItem forItem = items.get(1);
    assertEquals("item", forItem.getName());
    assertNotNull(forItem.getTask().getForTask(), "ForTask should be present");
  }

  @Test
  void testDoTaskMultipleTypes() {
    Workflow wf =
        WorkflowBuilder.workflow("flowMixed")
            .tasks(
                d ->
                    d.set("init", s -> s.expr("$.init = true"))
                        .forEach("items", f -> f.each("item").in("$.list"))
                        .switchC(
                            "choice",
                            sw -> {
                              // no-op configuration
                            })
                        .raise(
                            "alert",
                            r -> {
                              // no-op configuration
                            })
                        .fork(
                            "parallel",
                            f -> {
                              // no-op configuration
                            }))
            .build();

    List<TaskItem> items = wf.getDo();
    assertNotNull(items, "Do list must not be null");
    assertEquals(5, items.size(), "There should be five tasks");

    // set task
    TaskItem setItem = items.get(0);
    assertEquals("init", setItem.getName());
    assertNotNull(setItem.getTask().getSetTask(), "SetTask should be present");

    // forE task
    TaskItem forItem = items.get(1);
    assertEquals("items", forItem.getName());
    assertNotNull(forItem.getTask().getForTask(), "ForTask should be present");

    // switchTask
    TaskItem switchItem = items.get(2);
    assertEquals("choice", switchItem.getName());
    assertNotNull(switchItem.getTask().getSwitchTask(), "SwitchTask should be present");

    // raise task
    TaskItem raiseItem = items.get(3);
    assertEquals("alert", raiseItem.getName());
    assertNotNull(raiseItem.getTask().getRaiseTask(), "RaiseTask should be present");

    // fork task
    TaskItem forkItem = items.get(4);
    assertEquals("parallel", forkItem.getName());
    assertNotNull(forkItem.getTask().getForkTask(), "ForkTask should be present");
  }

  @Test
  void testDoTaskListenOne() {
    Workflow wf =
        WorkflowBuilder.workflow("flowListen")
            .tasks(
                d ->
                    d.listen(
                        "waitCheck",
                        l -> l.one(f -> f.with(p -> p.type("com.fake.pet").source("mySource")))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertNotNull(items, "Do list must not be null");
    assertEquals(1, items.size(), "There should be one task");

    TaskItem item = items.get(0);
    assertEquals("waitCheck", item.getName());
    ListenTask lt = item.getTask().getListenTask();
    assertNotNull(lt, "ListenTask should be present");
    OneEventConsumptionStrategy one = lt.getListen().getTo().getOneEventConsumptionStrategy();
    assertNotNull(one, "One consumption strategy should be set");
    EventFilter filter = one.getOne();
    assertNotNull(filter, "EventFilter should be present");
    assertEquals("com.fake.pet", filter.getWith().getType(), "Filter type should match");
  }

  @Test
  void testDoTaskEmitEvent() {
    Workflow wf =
        WorkflowBuilder.workflow("flowEmit")
            .tasks(
                d ->
                    d.emit(
                        "emitEvent",
                        e ->
                            e.event(
                                p ->
                                    p.source(URI.create("https://petstore.com"))
                                        .type("com.petstore.order.placed.v1")
                                        .data(
                                            Map.of(
                                                "client",
                                                Map.of(
                                                    "firstName", "Cruella", "lastName", "de Vil"),
                                                "items",
                                                List.of(
                                                    Map.of(
                                                        "breed", "dalmatian", "quantity", 101)))))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertNotNull(items, "Do list must not be null");
    assertEquals(1, items.size(), "There should be one emit task");

    TaskItem item = items.get(0);
    assertEquals("emitEvent", item.getName(), "TaskItem name should match");
    io.serverlessworkflow.api.types.EmitTask et = item.getTask().getEmitTask();
    assertNotNull(et, "EmitTask should be present");

    io.serverlessworkflow.api.types.EmitEventDefinition ed = et.getEmit().getEvent();
    assertNotNull(ed, "EmitEventDefinition should be present");
    io.serverlessworkflow.api.types.EventProperties props = ed.getWith();
    assertEquals(
        "https://petstore.com",
        props.getSource().getUriTemplate().getLiteralUri().toString(),
        "Source URI should match");
    assertEquals("com.petstore.order.placed.v1", props.getType(), "Event type should match");

    Object dataObj = props.getData().getObject();
    assertNotNull(dataObj, "Data object should be present");
    assertInstanceOf(Map.class, dataObj, "Data should be a Map");
    @SuppressWarnings("unchecked")
    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
    assertTrue(dataMap.containsKey("client"), "Data should contain 'client'");
    assertTrue(dataMap.containsKey("items"), "Data should contain 'items'");
  }

  @Test
  void testDoTaskTryCatchWithRetry() {
    Workflow wf =
        WorkflowBuilder.workflow("flowTry")
            .tasks(
                d ->
                    d.tryC(
                        "tryBlock",
                        t ->
                            t.tryHandler(tb -> tb.set("init", s -> s.expr("$.start = true")))
                                .catchHandler(
                                    c ->
                                        c.when("$.errorType == 'TEMP' ")
                                            .retry(
                                                r ->
                                                    r.when("$.retryCount < 3")
                                                        .limit(
                                                            l -> l.attempt(at -> at.count(3)))))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "There should be one try task");
    TaskItem item = items.get(0);
    assertEquals("tryBlock", item.getName());

    // Verify TryTask
    TryTask tryTask = item.getTask().getTryTask();
    assertNotNull(tryTask, "TryTask should be present");

    // Verify try handler tasks
    List<TaskItem> tryItems = tryTask.getTry();
    assertEquals(1, tryItems.size(), "Try handler should contain one task");
    TaskItem initItem = tryItems.get(0);
    assertEquals("init", initItem.getName());
    assertNotNull(initItem.getTask().getSetTask(), "SetTask in try handler should be present");

    // Verify catch configuration
    TryTaskCatch catchCfg = tryTask.getCatch();
    assertNotNull(catchCfg, "Catch configuration should be present");
    assertEquals("$.errorType == 'TEMP' ", catchCfg.getWhen());

    RetryPolicy retry = catchCfg.getRetry().getRetryPolicyDefinition();
    assertNotNull(retry, "RetryPolicy should be defined");
    assertEquals("$.retryCount < 3", retry.getWhen());
    RetryLimitAttempt attempt = retry.getLimit().getAttempt();
    assertEquals(3, attempt.getCount());
  }

  @Test
  void testDoTaskTryCatchErrorsFiltering() {
    Workflow wf =
        WorkflowBuilder.workflow("flowCatch")
            .tasks(
                d ->
                    d.tryC(
                        "tryBlock",
                        t ->
                            t.tryHandler(tb -> tb.set("foo", s -> s.expr("$.foo = 'bar'")))
                                .catchHandler(
                                    c ->
                                        c.exceptWhen("$.status == 500")
                                            .errorsWith(
                                                eb ->
                                                    eb.type("ServerError")
                                                        .status(500)
                                                        .instance("http://errors/5xx")))))
            .build();

    TaskItem item = wf.getDo().get(0);
    TryTask tryTask = item.getTask().getTryTask();
    TryTaskCatch catchCfg = tryTask.getCatch();

    // exceptWhen should match
    assertEquals("$.status == 500", catchCfg.getExceptWhen());

    CatchErrors errors = catchCfg.getErrors();
    assertNotNull(errors, "CatchErrors should be present");
    ErrorFilter filter = errors.getWith();
    assertEquals("ServerError", filter.getType());
    assertEquals(500, filter.getStatus());
    assertEquals("http://errors/5xx", filter.getInstance());
  }

  @Test
  void testWorkflowInputExternalSchema() {
    String uri = "http://example.com/schema";
    Workflow wf =
        WorkflowBuilder.workflow("wfInput").input(i -> i.from("$.data").schema(uri)).build();

    assertNotNull(wf.getInput(), "Input must be set");
    assertEquals("$.data", wf.getInput().getFrom().getString());
    assertNotNull(wf.getInput().getSchema().getSchemaExternal(), "External schema must be set");
    String resolved =
        wf.getInput()
            .getSchema()
            .getSchemaExternal()
            .getResource()
            .getEndpoint()
            .getUriTemplate()
            .getLiteralUri()
            .toString();
    assertEquals(uri, resolved, "Schema URI should match");
  }

  @Test
  void testWorkflowOutputExternalSchemaAndAs() {
    String uri = "http://example.org/output-schema";
    Workflow wf =
        WorkflowBuilder.workflow("wfOutput").output(o -> o.as("$.result").schema(uri)).build();

    assertNotNull(wf.getOutput(), "Output must be set");
    assertEquals("$.result", wf.getOutput().getAs().getString());
    assertNotNull(wf.getOutput().getSchema().getSchemaExternal(), "External schema must be set");
    String resolved =
        wf.getOutput()
            .getSchema()
            .getSchemaExternal()
            .getResource()
            .getEndpoint()
            .getUriTemplate()
            .getLiteralUri()
            .toString();
    assertEquals(uri, resolved, "Schema URI should match");
  }

  @Test
  void testWorkflowOutputInlineSchemaAndAsObject() {
    Map<String, Object> inline = Map.of("foo", "bar");
    Workflow wf =
        WorkflowBuilder.workflow().output(o -> o.as(Map.of("ok", true)).schema(inline)).build();

    assertNotNull(wf.getOutput(), "Output must be set");
    assertInstanceOf(Map.class, wf.getOutput().getAs().getObject(), "As object must be a Map");
    assertNotNull(wf.getOutput().getSchema().getSchemaInline(), "Inline schema must be set");
  }

  @Test
  void testWorkflowInputInlineSchemaAndFromObject() {
    Map<String, Object> inline = Map.of("nested", List.of(1, 2, 3));
    Workflow wf = WorkflowBuilder.workflow().input(i -> i.from(inline).schema(inline)).build();

    assertNotNull(wf.getInput(), "Input must be set");
    assertInstanceOf(Map.class, wf.getInput().getFrom().getObject(), "From object must be a Map");
    assertNotNull(wf.getInput().getSchema().getSchemaInline(), "Inline schema must be set");
  }

  @Test
  void testDoTaskCallHTTPBasic() {
    Workflow wf =
        WorkflowBuilder.workflow("flowCallBasic")
            .tasks(
                d ->
                    d.callHTTP(
                        "basicCall",
                        c ->
                            c.method("POST")
                                .endpoint(URI.create("http://example.com/api"))
                                .body(Map.of("foo", "bar"))))
            .build();
    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "Should have one HTTP call task");
    TaskItem ti = items.get(0);
    assertEquals("basicCall", ti.getName());
    CallHTTP call = ti.getTask().getCallTask().getCallHTTP();
    assertNotNull(call, "CallHTTP should be present");
    assertEquals("POST", call.getWith().getMethod());
    assertEquals(
        URI.create("http://example.com/api"),
        call.getWith().getEndpoint().getUriTemplate().getLiteralUri());
    assertInstanceOf(Map.class, call.getWith().getBody(), "Body should be the Map provided");
  }

  @Test
  void testDoTaskCallHTTPHeadersConsumerAndMap() {
    Workflow wf =
        WorkflowBuilder.workflow("flowCallHeaders")
            .tasks(
                d ->
                    d.callHTTP(
                        "hdrCall",
                        c ->
                            c.method("GET")
                                .endpoint("${uriExpr}")
                                .headers(h -> h.header("A", "1").header("B", "2"))))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    HTTPHeaders hh = call.getWith().getHeaders().getHTTPHeaders();
    assertEquals("1", hh.getAdditionalProperties().get("A"));
    assertEquals("2", hh.getAdditionalProperties().get("B"));

    Workflow wf2 =
        WorkflowBuilder.workflow()
            .tasks(
                d ->
                    d.callHTTP(
                        c ->
                            c.method("GET").endpoint("expr").headers(Map.of("X", "10", "Y", "20"))))
            .build();
    CallHTTP call2 = wf2.getDo().get(0).getTask().getCallTask().getCallHTTP();
    HTTPHeaders hh2 = call2.getWith().getHeaders().getHTTPHeaders();
    assertEquals("10", hh2.getAdditionalProperties().get("X"));
    assertEquals("20", hh2.getAdditionalProperties().get("Y"));
  }

  @Test
  void testDoTaskCallHTTPQueryConsumerAndMap() {
    Workflow wf =
        WorkflowBuilder.workflow("flowCallQuery")
            .tasks(
                d ->
                    d.callHTTP(
                        "qryCall",
                        c ->
                            c.method("GET")
                                .endpoint("exprUri")
                                .query(q -> q.query("k1", "v1").query("k2", "v2"))))
            .build();
    HTTPQuery hq =
        wf.getDo().get(0).getTask().getCallTask().getCallHTTP().getWith().getQuery().getHTTPQuery();
    assertEquals("v1", hq.getAdditionalProperties().get("k1"));
    assertEquals("v2", hq.getAdditionalProperties().get("k2"));

    Workflow wf2 =
        WorkflowBuilder.workflow()
            .tasks(
                d ->
                    d.callHTTP(
                        c -> c.method("GET").endpoint("uri").query(Map.of("q1", "x", "q2", "y"))))
            .build();
    HTTPQuery hq2 =
        wf2.getDo()
            .get(0)
            .getTask()
            .getCallTask()
            .getCallHTTP()
            .getWith()
            .getQuery()
            .getHTTPQuery();
    assertEquals("x", hq2.getAdditionalProperties().get("q1"));
    assertEquals("y", hq2.getAdditionalProperties().get("q2"));
  }

  @Test
  void testDoTaskCallHTTPRedirectAndOutput() {
    Workflow wf =
        WorkflowBuilder.workflow("flowCallOpts")
            .tasks(
                d ->
                    d.callHTTP(
                        "optCall",
                        c ->
                            c.method("DELETE")
                                .endpoint("expr")
                                .redirect(true)
                                .output(HTTPArguments.HTTPOutput.RESPONSE)))
            .build();
    CallHTTP call = wf.getDo().get(0).getTask().getCallTask().getCallHTTP();
    assertTrue(call.getWith().isRedirect(), "Redirect should be true");
    assertEquals(
        HTTPArguments.HTTPOutput.RESPONSE,
        call.getWith().getOutput(),
        "Output should be overridden");
  }
}
