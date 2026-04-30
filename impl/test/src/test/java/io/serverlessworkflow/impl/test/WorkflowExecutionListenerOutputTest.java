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
package io.serverlessworkflow.impl.test;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code instanceData().output()} is populated inside {@code onWorkflowCompleted}.
 *
 * <p>For synchronous workflows the {@code CompletableFuture} chain in {@code
 * WorkflowMutableInstance.startExecution()} completes inline on the calling thread. {@code
 * futureRef} is set only after the chain returns, so {@code instanceData().output()} — which routes
 * through {@code futureRef.get().join()} — was returning {@code null} when called from inside the
 * hook. The fix stores the output in {@code whenSuccess()} before the completion event fires.
 */
class WorkflowExecutionListenerOutputTest {

  @Test
  void instanceDataOutputIsAvailableInsideOnWorkflowCompleted() throws IOException {
    AtomicReference<WorkflowModel> capturedOutput = new AtomicReference<>();

    WorkflowExecutionListener listener =
        new WorkflowExecutionListener() {
          @Override
          public void onWorkflowCompleted(WorkflowCompletedEvent event) {
            capturedOutput.set(event.workflowContext().instanceData().output());
          }
        };

    try (WorkflowApplication app = WorkflowApplication.builder().withListener(listener).build()) {
      WorkflowModel result =
          app.workflowDefinition(
                  readWorkflowFromClasspath("workflows-samples/simple-expression.yaml"))
              .instance(Map.of())
              .start()
              .join();

      assertThat(result).isNotNull();
      assertThat(capturedOutput.get())
          .as(
              "instanceData().output() must be non-null inside onWorkflowCompleted — "
                  + "it was null because futureRef is set after startExecution() returns, "
                  + "which is after the synchronous CompletableFuture chain completes")
          .isNotNull();
      assertThat(capturedOutput.get().asMap())
          .as("instanceData().output() inside the hook must equal the workflow's final output")
          .isEqualTo(result.asMap());
    }
  }

  @Test
  void eventOutputMatchesInstanceDataOutput() throws IOException {
    AtomicReference<WorkflowModel> eventOutput = new AtomicReference<>();
    AtomicReference<WorkflowModel> instanceOutput = new AtomicReference<>();

    WorkflowExecutionListener listener =
        new WorkflowExecutionListener() {
          @Override
          public void onWorkflowCompleted(WorkflowCompletedEvent event) {
            eventOutput.set(event.output());
            instanceOutput.set(event.workflowContext().instanceData().output());
          }
        };

    try (WorkflowApplication app = WorkflowApplication.builder().withListener(listener).build()) {
      app.workflowDefinition(readWorkflowFromClasspath("workflows-samples/simple-expression.yaml"))
          .instance(Map.of())
          .start()
          .join();

      assertThat(eventOutput.get())
          .as("event.output() must be non-null — it is passed directly to WorkflowCompletedEvent")
          .isNotNull();
      assertThat(instanceOutput.get())
          .as(
              "instanceData().output() must equal event.output() — both should carry the same value")
          .isEqualTo(eventOutput.get());
    }
  }
}
