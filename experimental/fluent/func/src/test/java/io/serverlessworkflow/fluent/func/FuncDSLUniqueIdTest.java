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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withUniqueId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.dsl.UniqueIdBiFunction;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowPosition;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that withUniqueId/agent wrap the user's function so that, at runtime, the first argument
 * is a "unique id" composed as instanceId + "-" + jsonPointer (e.g., inst-123-/do/0/task).
 */
class FuncDSLUniqueIdTest {

  @SuppressWarnings("unchecked")
  private static JavaFilterFunction<Object, Object> extractJavaFilterFunction(CallJava callJava) {
    if (callJava instanceof CallJava.CallJavaFilterFunction<?, ?> f) {
      return (JavaFilterFunction<Object, Object>) f.function();
    }
    fail("CallTask is not a CallJavaFilterFunction; DSL contract may have changed.");
    return null; // unreachable
  }

  @Test
  @DisplayName(
      "withUniqueId(name, fn, in) composes uniqueId = instanceId-jsonPointer and passes it")
  void withUniqueId_uses_json_pointer_for_unique_id() throws Exception {
    AtomicReference<String> receivedUniqueId = new AtomicReference<>();
    AtomicReference<String> receivedPayload = new AtomicReference<>();

    UniqueIdBiFunction<String, String> fn =
        (uniqueId, payload) -> {
          receivedUniqueId.set(uniqueId);
          receivedPayload.set(payload);
          return payload.toUpperCase();
        };

    Workflow wf =
        FuncWorkflowBuilder.workflow("wf-unique-named")
            .tasks(withUniqueId("notify", fn, String.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "one task expected");
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    CallJava cj = (CallJava) t.getCallTask().get();
    var jff = extractJavaFilterFunction(cj);
    assertNotNull(jff, "JavaFilterFunction must be present for withUniqueId");

    // Mockito stubs for runtime contexts
    WorkflowInstanceData inst = mock(WorkflowInstanceData.class);
    when(inst.id()).thenReturn("inst-123");

    WorkflowContextData wctx = mock(WorkflowContextData.class);
    when(wctx.instanceData()).thenReturn(inst);

    // Use JSON Pointer for the unique component instead of task name
    final String pointer = "/do/0/task";
    WorkflowPosition pos = mock(WorkflowPosition.class);
    when(pos.jsonPointer()).thenReturn(pointer);

    TaskContextData tctx = mock(TaskContextData.class);
    when(tctx.position()).thenReturn(pos);

    Object result = jff.apply("hello", wctx, tctx);

    assertEquals(
        "inst-123-" + pointer, receivedUniqueId.get(), "uniqueId must be instanceId-jsonPointer");
    assertEquals(
        "hello", receivedPayload.get(), "payload should be forwarded to the user function");
    assertEquals("HELLO", result, "wrapped function result should be returned");
  }

  @Test
  @DisplayName("agent(fn, in) composes uniqueId = instanceId-jsonPointer and passes it")
  void agent_uses_json_pointer_for_unique_id() throws Exception {
    AtomicReference<String> receivedUniqueId = new AtomicReference<>();
    AtomicReference<Integer> receivedPayload = new AtomicReference<>();

    UniqueIdBiFunction<Integer, Integer> fn =
        (uniqueId, payload) -> {
          receivedUniqueId.set(uniqueId);
          receivedPayload.set(payload);
          return payload + 1;
        };

    Workflow wf = FuncWorkflowBuilder.workflow("wf-agent").tasks(agent(fn, Integer.class)).build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "one task expected");
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    CallJava cj = (CallJava) t.getCallTask().get();
    var jff = extractJavaFilterFunction(cj);
    assertNotNull(jff, "JavaFilterFunction must be present for agent/withUniqueId");

    WorkflowInstanceData inst = mock(WorkflowInstanceData.class);
    when(inst.id()).thenReturn("wf-999");

    WorkflowContextData wctx = mock(WorkflowContextData.class);
    when(wctx.instanceData()).thenReturn(inst);

    final String pointer = "/do/0/task";
    WorkflowPosition pos = mock(WorkflowPosition.class);
    when(pos.jsonPointer()).thenReturn(pointer);

    TaskContextData tctx = mock(TaskContextData.class);
    when(tctx.position()).thenReturn(pos);

    Object result = jff.apply(41, wctx, tctx);

    assertEquals(
        "wf-999-" + pointer, receivedUniqueId.get(), "uniqueId must be instanceId-jsonPointer");
    assertEquals(41, receivedPayload.get(), "payload should be forwarded to the user function");
    assertEquals(42, result, "wrapped function result should be returned");
  }
}
