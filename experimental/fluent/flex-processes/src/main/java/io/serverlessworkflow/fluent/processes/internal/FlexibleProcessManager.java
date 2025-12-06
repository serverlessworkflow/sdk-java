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
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class FlexibleProcessManager {

  private final Map<Activity, TaskExecutor<?>> executors;
  private final FlexibleProcess flexibleProcess;

  public FlexibleProcessManager(
      FlexibleProcess flexibleProcess, Map<Activity, TaskExecutor<?>> executors) {
    this.flexibleProcess = flexibleProcess;
    this.executors = executors;
  }

  public void run(
      WorkflowContext workflowContext, Optional<TaskContext> parentContext, WorkflowModel input) {
    boolean exit = flexibleProcess.getExitCondition().test(input);
    int counter = flexibleProcess.getMaxAttempts();
    while (!exit) {
      Map<Activity, TaskExecutor<?>> availableExecutors = getExecutors(input);
      if (availableExecutors.isEmpty()) {
        return;
      }
      Queue<Map.Entry<Activity, TaskExecutor<?>>> executorQueue =
          new LinkedList<>(availableExecutors.entrySet());
      while (!executorQueue.isEmpty()) {
        Map.Entry<Activity, TaskExecutor<?>> entry = executorQueue.poll();
        Activity activity = entry.getKey();
        TaskExecutor<?> executor = entry.getValue();
        try {
          executor
              .apply(workflowContext, parentContext, input)
              .join(); // blocking, because we run flexible process one by one
          if (activity.getPostAction() != null) {
            activity.getPostAction().accept(input);
          }
          activity.setExecuted();
        } catch (Exception e) {
          throw new RuntimeException("Error executing activity: " + activity.getName(), e);
        }
        exit = flexibleProcess.getExitCondition().test(input);
        if (exit) {
          break;
        }
      }
      counter--;
      if (counter <= 0) {
        break;
      }
    }
  }

  private Map<Activity, TaskExecutor<?>> getExecutors(WorkflowModel input) {
    return executors.entrySet().stream()
        .filter(activity -> activity.getKey().isRepeatable() || !activity.getKey().isExecuted())
        .filter(e -> e.getKey().getEntryCondition().test(input))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
