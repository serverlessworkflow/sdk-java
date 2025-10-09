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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.caseDefault;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.caseOf;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emit;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.event;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.fn;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.forEach;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchCase;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.tasks;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.to;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toAll;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toAny;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.cloudevents.CloudEventData;
import io.cloudevents.core.data.BytesCloudEventData;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FuncWorkflowBuilder + FuncDSL shortcuts (no JQ set expressions). */
class FuncDSLTest {

  // ---------- Shortcuts coverage ----------

  @Test
  @DisplayName("function(fn) and function(fn, Class) add CallTask")
  void functionOverloads_addCallTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("functionOverloads")
            .tasks(function((Long v) -> v + 10, Long.class), function(String::length, String.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(2, items.size());
    assertNotNull(items.get(0).getTask().getCallTask());
    assertNotNull(items.get(1).getTask().getCallTask());
  }

  @Test
  @DisplayName("emit(event(type, fn)) uses functional event data (inferred type)")
  void emit_event_inferredFn() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("emitFn")
            .tasks(emit(event("UserCreated", (String s) -> BytesCloudEventData.wrap(s.getBytes()))))
            .build();

    Task t = wf.getDo().get(0).getTask();
    assertNotNull(t.getEmitTask());
    assertNotNull(t.getEmitTask().getEmit().getEvent());
    assertNotNull(t.getEmitTask().getEmit().getEvent().getWith().getData());
    assertNull(t.getEmitTask().getEmit().getEvent().getWith().getData().getRuntimeExpression());
  }

  @Test
  @DisplayName("emit(type, fn) convenience")
  void emit_type_fn_shortcut() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("emitShortcut")
            .tasks(emit("Ping", (String s) -> BytesCloudEventData.wrap(s.getBytes())))
            .build();

    assertNotNull(wf.getDo().get(0).getTask().getEmitTask());
  }

  @Test
  @DisplayName("switchCase(cases(on(...), onDefault(...))) produces SwitchTask")
  void switchCase_cases_caseOf_and_default() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("switchShortcuts")
            .tasks(
                switchCase(
                    caseOf((Boolean b) -> b).then("thenTask"), caseDefault(FlowDirectiveEnum.END)))
            .build();

    assertNotNull(wf.getDo().get(0).getTask().getSwitchTask());
  }

  @Test
  @DisplayName("switchWhen / switchWhenOrElse sugar")
  void switchWhen_sugar() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("switchSugar")
            .tasks(
                switchWhen((Integer v) -> v > 0, "positive"),
                switchWhenOrElse((Integer v) -> v == 0, "zero", FlowDirectiveEnum.END))
            .build();

    assertNotNull(wf.getDo().get(0).getTask().getSwitchTask());
    assertNotNull(wf.getDo().get(1).getTask().getSwitchTask());
  }

  @Test
  @DisplayName("forEach(Collection) with body -> ForTaskFunction")
  void forEach_constantCollection() {
    List<String> col = List.of("a", "b", "c");

    Workflow wf =
        FuncWorkflowBuilder.workflow("foreachConstant")
            .tasks(forEach(col, tasks(function(String::toUpperCase, String.class))))
            .build();

    Task loopHolder = wf.getDo().get(0).getTask();
    assertNotNull(loopHolder.getForTask());
    ForTaskFunction fn = (ForTaskFunction) loopHolder.getForTask();
    assertEquals(1, fn.getDo().size());
    assertNotNull(fn.getDo().get(0).getTask().getCallTask());
  }

  @Test
  @DisplayName("forEach(Function<T,Collection<?>>) matches builder signature and nests body")
  void forEach_functionSignature() {
    Function<Integer, Collection<?>> collectionF = ctx -> Arrays.asList(1, 2, 3);

    Workflow wf =
        FuncWorkflowBuilder.workflow("foreachFunction")
            .tasks(forEach(collectionF, tasks(function(Integer::toHexString, Integer.class))))
            .build();

    Task loopHolder = wf.getDo().get(0).getTask();
    assertNotNull(loopHolder.getForTask());
    ForTaskFunction fn = (ForTaskFunction) loopHolder.getForTask();
    assertEquals(1, fn.getDo().size());
    assertNotNull(fn.getDo().get(0).getTask().getCallTask());
  }

  @Test
  @DisplayName("doTasks(function, emit, switchCase) preserves order")
  void doTasks_compositionOrder() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("composition")
            .tasks(
                function((Integer x) -> x + 1, Integer.class),
                emit("Ping", (String s) -> BytesCloudEventData.wrap(s.getBytes())),
                switchCase(caseOf((Boolean b) -> b).then("ok"), FuncDSL.caseDefault("fallback")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(3, items.size());
    assertNotNull(items.get(0).getTask().getCallTask());
    assertNotNull(items.get(1).getTask().getEmitTask());
    assertNotNull(items.get(2).getTask().getSwitchTask());
  }

  @Test
  @DisplayName("fn shortcut can be used directly inside callFn")
  void fn_shortcut_in_callFn() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("fnShortcut")
            .tasks(d -> d.callFn("calc", fn((Double v) -> v * 2, Double.class)))
            .build();

    assertNotNull(wf.getDo().get(0).getTask().getCallTask());
  }

  @Test
  @DisplayName("Java style forE with collection + whileC builds ForTaskFunction")
  void javaForEach_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("javaLoopFlow")
            .tasks(
                d ->
                    d.forEach(
                        j ->
                            j.collection(ctx -> List.of("a", "b", "c"))
                                .whileC((String val, Object ctx) -> !val.equals("c"))
                                .tasks(
                                    inner ->
                                        inner.callFn(
                                            c -> {
                                              /* body */
                                            }))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    TaskItem loopItem = items.get(0);
    Task task = loopItem.getTask();

    assertNotNull(task.getForTask(), "Java ForTaskFunction should be present");

    ForTaskFunction fn = (ForTaskFunction) task.getForTask();
    assertNotNull(fn.getDo(), "Nested 'do' list inside ForTaskFunction should be populated");
    assertEquals(1, fn.getDo().size());
    Task nested = fn.getDo().get(0).getTask();
    assertNotNull(nested.getCallTask());
  }

  @Test
  @DisplayName("Mixed spec and Java loops in one workflow (no set)")
  void mixedLoops_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("mixed")
            .tasks(
                d ->
                    d.forEach(f -> f.each("item").in("$.array")) // spec
                        .forEach(j -> j.collection(ctx -> List.of(1, 2, 3))) // java
                )
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(2, items.size());

    Task specLoop = items.get(0).getTask();
    Task javaLoop = items.get(1).getTask();

    assertNotNull(specLoop.getForTask());
    assertNotNull(javaLoop.getForTask());
  }

  @Test
  @DisplayName("Java functional exportAsFn/inputFrom/outputAs wrappers (no literal set)")
  void javaFunctionalIO_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("fnIO")
            .tasks(
                d ->
                    d.forEach(
                        j ->
                            j.collection(ctx -> List.of("x", "y"))
                                .tasks(
                                    inner ->
                                        inner.callFn(
                                            c -> {
                                              /* calc */
                                            }))
                                .exportAs(item -> Map.of("computed", 42))
                                .outputAs(item -> Map.of("out", true))))
            .build();

    assertEquals(1, wf.getDo().size());

    Task forTaskFnHolder = wf.getDo().get(0).getTask();
    ForTaskFunction fn = (ForTaskFunction) forTaskFnHolder.getForTask();
    assertNotNull(fn);

    List<TaskItem> nested = fn.getDo();
    assertEquals(1, nested.size());
    assertNotNull(nested.get(0).getTask().getCallTask());

    // Structural checks for function-based export/output
    assertNotNull(fn.getExport(), "Export should be set via functional variant");
    if (fn.getExport().getAs() != null) {
      assertNull(fn.getExport().getAs().getString(), "Export 'as' should not be a literal string");
    }

    if (fn.getOutput() != null && fn.getOutput().getAs() != null) {
      assertNull(fn.getOutput().getAs().getString(), "Output 'as' should not be a literal string");
    }
  }

  @Test
  @DisplayName("callFn task added and retains name + CallTask union")
  void callJavaTask_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("callJavaFlow")
            .tasks(
                d ->
                    d.callFn(
                        "invokeHandler",
                        cj -> {
                          // configure your FuncCallTaskBuilder here
                        }))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    TaskItem ti = items.get(0);

    assertEquals("invokeHandler", ti.getName());
    Task task = ti.getTask();
    assertNotNull(task.getCallTask(), "CallTask should be present for callFn");
  }

  @Test
  @DisplayName("switchCaseFn (Java variant) without spec set branch")
  void switchCaseJava_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("switchJava")
            .tasks(
                d ->
                    d.switchCase(
                        sw -> {
                          // configure Java switch builder (cases / predicates)
                        }))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task switchTask = items.get(0).getTask();
    assertNotNull(switchTask.getSwitchTask(), "SwitchTask union should be present");
  }

  @Test
  @DisplayName("Composite: java forE + nested callFn (no set)")
  void compositeScenario_noSet() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("composite")
            .tasks(
                d ->
                    d.forEach(
                        j ->
                            j.collection(ctx -> List.of("a", "b"))
                                .tasks(
                                    inner ->
                                        inner
                                            .callFn(
                                                cj -> {
                                                  /* customizing Java call */
                                                })
                                            .callFn(
                                                cj -> {
                                                  /* second step */
                                                }))))
            .build();

    assertEquals(1, wf.getDo().size());

    Task loopHolder = wf.getDo().get(0).getTask();
    ForTaskFunction fn = (ForTaskFunction) loopHolder.getForTask();
    assertNotNull(fn);

    List<TaskItem> nested = fn.getDo();
    assertEquals(2, nested.size());

    Task nestedCall1 = nested.get(0).getTask();
    Task nestedCall2 = nested.get(1).getTask();

    assertNotNull(nestedCall1.getCallTask());
    assertNotNull(nestedCall2.getCallTask());
  }

  @Test
  @DisplayName("listen(toAny(types...)) produces ListenTask")
  void listen_toAny_minimal() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("listenAny")
            .tasks(listen(toAny("org.acme.email.approved", "org.acme.email.denied")))
            .build();

    assertEquals(1, wf.getDo().size());
    Task t = wf.getDo().get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask should be present");
  }

  @Test
  @DisplayName("listen(name, toAll(types...).until(expr)) is named and has ListenTask")
  void listen_named_toAll_until() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("listenAllNamed")
            .tasks(
                listen(
                    "waitForAll",
                    toAll("org.acme.signal.one", "org.acme.signal.two")
                        .until((CloudEventData e) -> e.toString().isEmpty(), CloudEventData.class)))
            .build();

    assertEquals(1, wf.getDo().size());
    TaskItem ti = wf.getDo().get(0);
    assertEquals("waitForAll", ti.getName(), "Listen task should preserve given name");
    assertNotNull(ti.getTask().getListenTask(), "ListenTask should be present");
  }

  @Test
  @DisplayName("listen(toOne(type)) produces ListenTask")
  void listen_toOne() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("listenOne")
            .tasks(listen(toOne("org.acme.email.review.required")))
            .build();

    assertEquals(1, wf.getDo().size());
    assertNotNull(wf.getDo().get(0).getTask().getListenTask());
  }

  @Test
  @DisplayName("emit -> listen -> emit ordering with FuncDSL listen fluent")
  void emit_listen_emit_order() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("emitListenEmit")
            .tasks(
                emit(
                    "org.acme.email.started",
                    (String s) -> BytesCloudEventData.wrap(s.getBytes(UTF_8))),
                listen(toAny("org.acme.email.approved", "org.acme.email.denied")),
                emit(
                    "org.acme.email.finished",
                    (String s) -> BytesCloudEventData.wrap(s.getBytes(UTF_8))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(3, items.size(), "Three steps should be composed in order");

    Task first = items.get(0).getTask();
    Task second = items.get(1).getTask();
    Task third = items.get(2).getTask();

    assertNotNull(first.getEmitTask(), "1st is EmitTask");
    assertNotNull(second.getListenTask(), "2nd is ListenTask");
    assertNotNull(third.getEmitTask(), "3rd is EmitTask");
  }

  @Test
  @DisplayName(
      "Functional parity of agentic example: callFn -> switchCase -> emit -> listen -> emit")
  void functional_parity_example() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("emailDrafterFunctional")
            .tasks( // parseDraft
                function(String::trim, String.class),
                // policyCheck – pretend it maps string->decision code (0: auto, 1: needs review)
                function((String parsed) -> parsed.isEmpty() ? 1 : 0, String.class),
                // needsHumanReview? -> requestReview | emailFinished
                switchCase(
                    "needsHumanReview?",
                    FuncDSL.caseOf((Integer decision) -> decision != 0, Integer.class)
                        .then("requestReview"),
                    FuncDSL.caseDefault("emailFinished")),
                // emit review request (named branch)
                emit(
                    "requestReview",
                    "org.acme.email.request",
                    (String payload) -> BytesCloudEventData.wrap(payload.getBytes(UTF_8))),
                // wait for any of approved/denied
                listen("waitForReview", toAny("org.acme.email.approved", "org.acme.email.denied")),
                // finished event
                emit(
                    "emailFinished",
                    "org.acme.email.finished",
                    (String payload) -> BytesCloudEventData.wrap(payload.getBytes(UTF_8))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(6, items.size());

    assertNotNull(items.get(0).getTask().getCallTask()); // parseDraft
    assertNotNull(items.get(1).getTask().getCallTask()); // policyCheck
    assertNotNull(items.get(2).getTask().getSwitchTask()); // needsHumanReview?
    assertNotNull(items.get(3).getTask().getEmitTask()); // requestReview
    assertNotNull(items.get(4).getTask().getListenTask()); // waitForReview
    assertNotNull(items.get(5).getTask().getEmitTask()); // emailFinished

    assertEquals("needsHumanReview?", items.get(2).getName());
    assertEquals("requestReview", items.get(3).getName());
    assertEquals("waitForReview", items.get(4).getName());
    assertEquals("emailFinished", items.get(5).getName());
  }

  @Test
  @DisplayName("listen(to().any(...).until(...)) builds ListenTask with chained spec")
  void listen_with_to_chaining() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("listenChained")
            .tasks(
                listen(
                    to().any(event("org.acme.sig.one"), event("org.acme.sig.two"))
                        .until((CloudEventData e) -> e.toString().isEmpty(), CloudEventData.class)))
            .build();

    assertEquals(1, wf.getDo().size());
    assertNotNull(wf.getDo().get(0).getTask().getListenTask());
    assertNotNull(
        wf.getDo()
            .get(0)
            .getTask()
            .getListenTask()
            .getListen()
            .getTo()
            .getAnyEventConsumptionStrategy()
            .getUntil());
  }
}
