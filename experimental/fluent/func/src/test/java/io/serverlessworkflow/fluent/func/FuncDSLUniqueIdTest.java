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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.dsl.UniqueIdBiFunction;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinitionData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that withUniqueId/agent wrap the user's function so that, at runtime, the first argument
 * is a "unique id" composed as instanceId + "-" + taskName.
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
  @DisplayName("withUniqueId(name, fn, in) composes uniqueId = instanceId-taskName and passes it")
  void withUniqueId_named_composes_and_passes_unique_id() throws Exception {
    AtomicReference<String> receivedUniqueId = new AtomicReference<>();
    AtomicReference<String> receivedPayload = new AtomicReference<>();

    // (uniqueId, payload) -> result; we capture inputs for assertion
    UniqueIdBiFunction<String, String> fn =
        (uniqueId, payload) -> {
          receivedUniqueId.set(uniqueId);
          receivedPayload.set(payload);
          return payload.toUpperCase();
        };

    Workflow wf =
        FuncWorkflowBuilder.workflow("wf-unique-named")
            .tasks(
                // important: NAME is provided → should appear in the uniqueId
                withUniqueId("notify", fn, String.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "one task expected");
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");

    String taskName = items.get(0).getName();
    assertEquals("notify", taskName, "task name should be set on the step");

    CallJava cj = (CallJava) t.getCallTask().get();
    var jff = extractJavaFilterFunction(cj);
    assertNotNull(jff, "JavaFilterFunction must be present for withUniqueId");

    // Invoke the wrapped function "as runtime" with fake contexts
    var wctx = new FakeWorkflowContextData("inst-123");
    var tctx = new FakeTaskContextData(taskName);

    // The JavaFilterFunction signature in your impl is (payload, wctx, tctx) -> result
    Object result = jff.apply("hello", wctx, tctx);

    assertEquals("inst-123-notify", receivedUniqueId.get(), "uniqueId must be instanceId-taskName");
    assertEquals(
        "hello", receivedPayload.get(), "payload should be forwarded to the user function");
    assertEquals("HELLO", result, "wrapped function result should be returned");
  }

  @Test
  @DisplayName("agent(fn, in) is sugar for withUniqueId(fn, in) and passes instanceId-taskName")
  void agent_unnamed_composes_and_passes_unique_id() throws Exception {
    AtomicReference<String> receivedUniqueId = new AtomicReference<>();
    AtomicReference<Integer> receivedPayload = new AtomicReference<>();

    UniqueIdBiFunction<Integer, Integer> fn =
        (uniqueId, payload) -> {
          receivedUniqueId.set(uniqueId);
          receivedPayload.set(payload);
          return payload + 1;
        };

    Workflow wf =
        FuncWorkflowBuilder.workflow("wf-agent")
            .tasks(
                // No explicit name here; builder should still set a task name,
                // which participates in the uniqueId (instanceId-taskName)
                agent(fn, Integer.class))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size(), "one task expected");
    Task t = items.get(0).getTask();
    assertNotNull(t.getCallTask(), "CallTask expected");
    String taskName = items.get(0).getName();
    assertNotNull(taskName, "task name should be assigned even if not explicitly provided");

    CallJava cj = (CallJava) t.getCallTask().get();
    var jff = extractJavaFilterFunction(cj);
    assertNotNull(jff, "JavaFilterFunction must be present for agent/withUniqueId");

    WorkflowContextData wctx = new FakeWorkflowContextData("wf-999");
    TaskContextData tctx = new FakeTaskContextData(taskName);

    Object result = jff.apply(41, wctx, tctx);

    assertEquals(
        "wf-999-" + taskName,
        receivedUniqueId.get(),
        "agent should compose uniqueId as instanceId-taskName");
    assertEquals(41, receivedPayload.get(), "payload should be forwarded to the user function");
    assertEquals(42, result, "wrapped function result should be returned");
  }

  /**
   * Minimal test doubles to satisfy the JavaFilterFunction call path. We only implement the members
   * used by the DSL composition: - wctx.instanceData().id() - tctx.taskName()
   */
  static final class FakeWorkflowContextData implements WorkflowContextData {
    private final String id;

    FakeWorkflowContextData(String id) {
      this.id = id;
    }

    @Override
    public WorkflowInstanceData instanceData() {
      // Provide just the id() accessor
      return new WorkflowInstanceData() {
        @Override
        public String id() {
          return id;
        }

        @Override
        public Instant startedAt() {
          return null;
        }

        @Override
        public Instant completedAt() {
          return null;
        }

        @Override
        public WorkflowModel input() {
          return null;
        }

        @Override
        public WorkflowStatus status() {
          return null;
        }

        @Override
        public WorkflowModel output() {
          return null;
        }

        @Override
        public WorkflowModel context() {
          return null;
        }

        @Override
        public <T> T outputAs(Class<T> clazz) {
          return null;
        }
      };
    }

    @Override
    public WorkflowModel context() {
      return null;
    }

    @Override
    public WorkflowDefinitionData definition() {
      return null;
    }
  }

  record FakeTaskContextData(String taskName) implements TaskContextData {

    @Override
    public WorkflowModel input() {
      return null;
    }

    @Override
    public WorkflowModel rawInput() {
      return null;
    }

    @Override
    public TaskBase task() {
      return null;
    }

    @Override
    public WorkflowModel rawOutput() {
      return null;
    }

    @Override
    public WorkflowModel output() {
      return null;
    }

    @Override
    public WorkflowPosition position() {
      return null;
    }

    @Override
    public Instant startedAt() {
      return null;
    }

    @Override
    public Instant completedAt() {
      return null;
    }
  }
}
