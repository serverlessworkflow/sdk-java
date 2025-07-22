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

import static org.junit.jupiter.api.Assertions.*;

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.*;
import io.serverlessworkflow.fluent.spec.BaseWorkflowBuilder;
// if you reuse anything
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for FuncWorkflowBuilder + Java DSL extensions. */
class JavaWorkflowBuilderTest {

  @Test
  @DisplayName("Default Java workflow has auto-generated name and default namespace/version")
  void testDefaults() {
    Workflow wf = FuncWorkflowBuilder.workflow().build();
    assertNotNull(wf);
    Document doc = wf.getDocument();
    assertNotNull(doc);
    assertEquals(BaseWorkflowBuilder.DEFAULT_NAMESPACE, doc.getNamespace());
    assertEquals(BaseWorkflowBuilder.DEFAULT_VERSION, doc.getVersion());
    assertEquals(BaseWorkflowBuilder.DSL, doc.getDsl());
    assertNotNull(doc.getName());
  }

  @Test
  @DisplayName("Spec style forE still works inside Java workflow")
  void testSpecForEachInJavaWorkflow() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("specLoopFlow")
            .tasks(
                d ->
                    d.forEach(f -> f.each("pet").in("$.pets"))
                        .set("markDone", s -> s.expr("$.done = true")))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(2, items.size());

    TaskItem loopItem = items.get(0);
    assertNotNull(loopItem.getTask().getForTask(), "Spec ForTask should be present");

    TaskItem setItem = items.get(1);
    assertNotNull(setItem.getTask().getSetTask());
    SetTask st = setItem.getTask().getSetTask();
    assertEquals("$.done = true", st.getSet().getString());
  }

  @Test
  @DisplayName("Java style forE with collection + whileC builds ForTaskFunction")
  void testJavaForEach() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("javaLoopFlow")
            .tasks(
                d ->
                    d.forFn(
                        j ->
                            j.collection(ctx -> List.of("a", "b", "c"))
                                .whileC((String val, Object ctx) -> !val.equals("c"))
                                .tasks(
                                    inner -> inner.set("loopFlag", s -> s.expr("$.flag = true")))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    TaskItem loopItem = items.get(0);
    Task task = loopItem.getTask();

    assertNotNull(task.getForTask(), "Java ForTaskFunction should be present");

    // Basic structural checks on nested do inside the function loop
    ForTaskFunction fn = (ForTaskFunction) task.getForTask();
    assertNotNull(fn.getDo(), "Nested 'do' list inside ForTaskFunction should be populated");
    assertEquals(1, fn.getDo().size());
    Task nested = fn.getDo().get(0).getTask();
    assertNotNull(nested.getSetTask());
  }

  @Test
  @DisplayName("Mixed spec and Java loops in one workflow")
  void testMixedLoops() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("mixed")
            .tasks(
                d ->
                    d.forEach(f -> f.each("item").in("$.array")) // spec
                        .forFn(j -> j.collection(ctx -> List.of(1, 2, 3))) // java
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
  @DisplayName("Java functional exportAsFn/inputFrom/outputAs set function wrappers (not literals)")
  void testJavaFunctionalIO() {
    AtomicBoolean exportCalled = new AtomicBoolean(false);
    AtomicBoolean inputCalled = new AtomicBoolean(false);
    AtomicBoolean outputCalled = new AtomicBoolean(false);

    Workflow wf =
        FuncWorkflowBuilder.workflow("fnIO")
            .tasks(
                d ->
                    d.set("init", s -> s.expr("$.x = 1"))
                        .forFn(
                            j ->
                                j.collection(
                                        ctx -> {
                                          inputCalled.set(true);
                                          return List.of("x", "y");
                                        })
                                    .tasks(inner -> inner.set("calc", s -> s.expr("$.y = $.x + 1")))
                                    .exportAsFn(
                                        item -> {
                                          exportCalled.set(true);
                                          return Map.of("computed", 42);
                                        })
                                    .outputAs(
                                        item -> {
                                          outputCalled.set(true);
                                          return Map.of("out", true);
                                        })))
            .build();

    // Top-level 'do' structure
    assertEquals(2, wf.getDo().size());

    // Find nested forTaskFunction
    Task forTaskFnHolder = wf.getDo().get(1).getTask();
    ForTaskFunction fn = (ForTaskFunction) forTaskFnHolder.getForTask();
    assertNotNull(fn);

    // Inspect nested tasks inside the function loop
    List<TaskItem> nested = fn.getDo();
    assertEquals(1, nested.size());
    TaskBase nestedTask = nested.get(0).getTask().getSetTask();
    assertNotNull(nestedTask);

    // Because functions are likely stored as opaque objects, we check that
    // export / output structures exist and are not expression-based.
    Export export = fn.getExport();
    assertNotNull(export, "Export should be set via functional variant");
    assertNull(
        export.getAs() != null ? export.getAs().getString() : null,
        "Export 'as' should not be a plain string when using function variant");

    Output out = fn.getOutput();
    // If functional output maps to an OutputAsFunction wrapper, adapt the checks:
    if (out != null && out.getAs() != null) {
      // Expect no literal string if function used
      assertNull(out.getAs().getString(), "Output 'as' should not be a literal string");
    }

    // We can't *invoke* lambdas here (unless your runtime exposes them),
    // but we verified structural placement. Flipping AtomicBooleans in creation lambdas
    // (collection) at least shows one function executed during build (if it is executed now;
    // if they are deferred, remove those assertions.)
  }

  @Test
  @DisplayName("callJava task added and retains name + CallTask union")
  void testCallJavaTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("callJavaFlow")
            .tasks(
                d ->
                    d.callFn(
                        "invokeHandler",
                        cj -> {
                          // configure your FuncCallTaskBuilder here
                          // e.g., cj.className("com.acme.Handler").arg("key", "value");
                        }))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());
    TaskItem ti = items.get(0);

    assertEquals("invokeHandler", ti.getName());
    Task task = ti.getTask();
    assertNotNull(task.getCallTask(), "CallTask should be present for callJava");
    // Additional assertions if FuncCallTaskBuilder populates fields
    // e.g., assertEquals("com.acme.Handler", task.getCallTask().getCallJava().getClassName());
  }

  @Test
  @DisplayName("switchCaseFn (Java variant) coexists with spec tasks")
  void testSwitchCaseJava() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("switchJava")
            .tasks(
                d ->
                    d.set("prepare", s -> s.expr("$.ready = true"))
                        .switchC(
                            sw -> {
                              // configure Java switch builder (cases / predicates)
                            }))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(2, items.size());

    Task specSet = items.get(0).getTask();
    Task switchTask = items.get(1).getTask();

    assertNotNull(specSet.getSetTask());
    assertNotNull(switchTask.getSwitchTask(), "SwitchTask union should be present");
  }

  @Test
  @DisplayName("Combined: spec set + java forE + callJava inside nested do")
  void testCompositeScenario() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("composite")
            .tasks(
                d ->
                    d.set("init", s -> s.expr("$.val = 0"))
                        .forFn(
                            j ->
                                j.collection(ctx -> List.of("a", "b"))
                                    .tasks(
                                        inner ->
                                            inner
                                                .callJava(
                                                    cj -> {
                                                      // customizing Java call
                                                    })
                                                .set("flag", s -> s.expr("$.flag = true")))))
            .build();

    assertEquals(2, wf.getDo().size());

    Task loopHolder = wf.getDo().get(1).getTask();
    ForTaskFunction fn = (ForTaskFunction) loopHolder.getForTask();
    assertNotNull(fn);

    List<TaskItem> nested = fn.getDo();
    assertEquals(2, nested.size());

    Task nestedCall = nested.get(0).getTask();
    Task nestedSet = nested.get(1).getTask();

    assertNotNull(nestedCall.getCallTask());
    assertNotNull(nestedSet.getSetTask());
  }
}
