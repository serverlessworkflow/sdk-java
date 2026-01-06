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
package io.serverlessworkflow.fluent.processes.internal;

import io.serverlessworkflow.fluent.processes.Activity;
import io.serverlessworkflow.fluent.processes.FlexibleProcess;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FlexibleProcessManager {

  private final Map<Activity, TaskExecutor<?>> executors;
  private final FlexibleProcess flexibleProcess;

  public FlexibleProcessManager(
      FlexibleProcess flexibleProcess, Map<Activity, TaskExecutor<?>> executors) {
    this.flexibleProcess = flexibleProcess;
    this.executors = executors;
  }

  public CompletableFuture<WorkflowModel> run(
      WorkflowContext workflowContext, Optional<TaskContext> parentContext, WorkflowModel input) {

    int maxAttempts = flexibleProcess.getMaxAttempts();
    CompletableFuture<WorkflowModel> promise = new CompletableFuture<>();
    runAttempt(workflowContext, parentContext, input, maxAttempts, promise);
    return promise;
  }

  private void runAttempt(
      WorkflowContext workflowContext,
      Optional<TaskContext> parentContext,
      WorkflowModel input,
      int remainingAttempts,
      CompletableFuture<WorkflowModel> promise) {

    if (promise.isDone()) {
      return;
    }

    if (flexibleProcess.getExitCondition().test(input)) {
      promise.complete(input);
      return;
    }

    if (remainingAttempts <= 0) {
      promise.complete(input);
      return;
    }

    Map<Activity, TaskExecutor<?>> availableExecutors = getExecutors(input);
    if (availableExecutors.isEmpty()) {
      promise.complete(input);
      return;
    }

    CompletableFuture<WorkflowModel> passFuture =
        runOnePassSequentially(workflowContext, parentContext, input, availableExecutors);

    passFuture.whenComplete(
        (updatedInput, ex) -> {
          if (ex != null) {
            promise.completeExceptionally(ex);
            return;
          }

          if (flexibleProcess.getExitCondition().test(updatedInput)) {
            promise.complete(updatedInput);
            return;
          }
          runAttempt(workflowContext, parentContext, updatedInput, remainingAttempts - 1, promise);
        });
  }

  private CompletableFuture<WorkflowModel> runOnePassSequentially(
      WorkflowContext workflowContext,
      Optional<TaskContext> parentContext,
      WorkflowModel input,
      Map<Activity, TaskExecutor<?>> availableExecutors) {

    return runNextExecutor(
        workflowContext, parentContext, input, availableExecutors.entrySet().iterator());
  }

  private CompletableFuture<WorkflowModel> runNextExecutor(
      WorkflowContext workflowContext,
      Optional<TaskContext> parentContext,
      WorkflowModel input,
      java.util.Iterator<Map.Entry<Activity, TaskExecutor<?>>> it) {

    if (flexibleProcess.getExitCondition().test(input) || !it.hasNext()) {
      return CompletableFuture.completedFuture(input);
    }

    Map.Entry<Activity, TaskExecutor<?>> entry = it.next();
    Activity activity = entry.getKey();
    TaskExecutor<?> executor = entry.getValue();

    return executor
        .apply(workflowContext, parentContext, input)
        .thenApply(
            taskContext -> {
              if (activity.getPostAction() != null) {
                activity.getPostAction().accept(input);
              }
              activity.setExecuted();
              return input;
            })
        .thenCompose(
            updatedInput -> runNextExecutor(workflowContext, parentContext, updatedInput, it));
  }

  private Map<Activity, TaskExecutor<?>> getExecutors(WorkflowModel input) {
    return executors.entrySet().stream()
        .filter(
            activity ->
                (activity.getKey().isRepeatable() || !activity.getKey().isExecuted())
                    && activity.getKey().getEntryCondition().test(input))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
