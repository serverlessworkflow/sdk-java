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
package io.serverlessworkflow.fluent.test;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class FuncCallAsyncTest {

  private void safeSleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private CompletableFuture<Integer> waitAsync(Integer waitTime) {
    return CompletableFuture.supplyAsync(
        () -> {
          safeSleep(waitTime);
          return 1;
        });
  }

  private Integer waitSync(Integer waitTime) {
    safeSleep(waitTime);
    return 1;
  }

  @Test
  void testCompletableCall() {
    runIt(FuncWorkflowBuilder.workflow("waitCompletable").tasks(function(this::waitAsync)).build());
  }

  @Test
  void testReferencedFunctionCall() {
    runIt(FuncWorkflowBuilder.workflow("waitReference").tasks(function(this::waitSync)).build());
  }

  @Test
  void testLambdaCall() {
    runIt(FuncWorkflowBuilder.workflow("waitLambda").tasks(function(v -> 1)).build());
  }

  private class TimeListener implements WorkflowExecutionListener {

    private AtomicLong startTime = new AtomicLong();

    @Override
    public void onWorkflowStarted(WorkflowStartedEvent ev) {
      startTime.set(System.currentTimeMillis());
    }
  }

  private void runIt(Workflow workflow) {
    TimeListener listener = new TimeListener();
    try (WorkflowApplication app = WorkflowApplication.builder().withListener(listener).build()) {
      final long waitTime = 200;
      WorkflowInstance instance = app.workflowDefinition(workflow).instance(waitTime);
      CompletableFuture<WorkflowModel> future = instance.start();
      assertThat(System.currentTimeMillis() - listener.startTime.get()).isLessThan(waitTime);
      assertThat(future.join().asNumber().map(Number::intValue).orElseThrow()).isEqualTo(1);
    }
  }
}
