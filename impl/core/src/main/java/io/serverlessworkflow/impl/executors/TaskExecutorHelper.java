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
package io.serverlessworkflow.impl.executors;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TaskExecutorHelper {
  private TaskExecutorHelper() {}

  public static CompletableFuture<JsonNode> processTaskList(
      TaskExecutor<?> taskExecutor,
      WorkflowContext context,
      Optional<TaskContext> parentTask,
      JsonNode input) {
    return taskExecutor
        .apply(context, parentTask, input)
        .thenApply(
            t -> {
              parentTask.ifPresent(p -> p.rawOutput(t.output()));
              return t.output();
            });
  }

  public static boolean isActive(WorkflowContext context) {
    return isActive(context.instance().status());
  }

  public static boolean isActive(WorkflowStatus status) {
    return status == WorkflowStatus.RUNNING || status == WorkflowStatus.WAITING;
  }

  public static TaskExecutor<?> createExecutorList(
      WorkflowPosition position,
      List<TaskItem> taskItems,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    Map<String, TaskExecutorBuilder<?>> executors =
        createExecutorBuilderList(position, taskItems, workflow, application, resourceLoader, "do");
    executors.values().forEach(t -> t.connect(executors));
    Iterator<TaskExecutorBuilder<?>> iter = executors.values().iterator();
    TaskExecutor<?> first = iter.next().build();
    while (iter.hasNext()) {
      iter.next().build();
    }
    return first;
  }

  public static Map<String, TaskExecutor<?>> createBranchList(
      WorkflowPosition position,
      List<TaskItem> taskItems,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    return createExecutorBuilderList(
            position, taskItems, workflow, application, resourceLoader, "branch")
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
  }

  private static Map<String, TaskExecutorBuilder<?>> createExecutorBuilderList(
      WorkflowPosition position,
      List<TaskItem> taskItems,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader,
      String containerName) {
    TaskExecutorFactory taskFactory = application.taskFactory();
    Map<String, TaskExecutorBuilder<?>> executors = new LinkedHashMap<>();
    position.addProperty(containerName);
    int index = 0;
    for (TaskItem item : taskItems) {
      position.addIndex(index++).addProperty(item.getName());
      TaskExecutorBuilder<?> taskExecutorBuilder =
          taskFactory.getTaskExecutor(
              position.copy(), item.getTask(), workflow, application, resourceLoader);
      executors.put(item.getName(), taskExecutorBuilder);
      position.back().back();
    }
    return executors;
  }
}
