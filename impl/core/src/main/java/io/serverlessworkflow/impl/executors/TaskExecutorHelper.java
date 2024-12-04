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
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.util.List;
import java.util.ListIterator;

public class TaskExecutorHelper {
  private TaskExecutorHelper() {}

  public static void processTaskList(
      List<TaskItem> tasks, WorkflowContext context, TaskContext<?> parentTask) {
    parentTask.position().addProperty("do");
    TaskContext<? extends TaskBase> currentContext = parentTask;
    if (!tasks.isEmpty()) {
      ListIterator<TaskItem> iter = tasks.listIterator();
      TaskItem nextTask = iter.next();
      while (nextTask != null && isActive(context)) {
        TaskItem task = nextTask;
        parentTask.position().addIndex(iter.previousIndex());
        currentContext = executeTask(context, parentTask, task, currentContext.output());
        FlowDirective flowDirective = currentContext.flowDirective();
        if (flowDirective.getFlowDirectiveEnum() != null) {
          switch (flowDirective.getFlowDirectiveEnum()) {
            case CONTINUE:
              nextTask = iter.hasNext() ? iter.next() : null;
              break;
            case END:
              context.instance().status(WorkflowStatus.COMPLETED);
              break;
            case EXIT:
              nextTask = null;
              break;
          }
        } else {
          nextTask = findTaskByName(iter, flowDirective.getString());
        }
        parentTask.position().back();
      }
    }
    parentTask.position().back();
    parentTask.rawOutput(currentContext.output());
  }

  public static boolean isActive(WorkflowContext context) {
    return isActive(context.instance().status());
  }

  public static boolean isActive(WorkflowStatus status) {
    return status == WorkflowStatus.RUNNING;
  }

  public static TaskContext<?> executeTask(
      WorkflowContext context, TaskContext<?> parentTask, TaskItem task, JsonNode input) {
    parentTask.position().addProperty(task.getName());
    TaskContext<?> result =
        context
            .definition()
            .taskExecutors()
            .computeIfAbsent(
                parentTask.position().jsonPointer(),
                k ->
                    context
                        .definition()
                        .taskFactory()
                        .getTaskExecutor(task.getTask(), context.definition()))
            .apply(context, parentTask, input);
    parentTask.position().back();
    return result;
  }

  private static TaskItem findTaskByName(ListIterator<TaskItem> iter, String taskName) {
    int currentIndex = iter.nextIndex();
    while (iter.hasPrevious()) {
      TaskItem item = iter.previous();
      if (item.getName().equals(taskName)) {
        return item;
      }
    }
    while (iter.nextIndex() < currentIndex) {
      iter.next();
    }
    while (iter.hasNext()) {
      TaskItem item = iter.next();
      if (item.getName().equals(taskName)) {
        return item;
      }
    }
    throw new IllegalArgumentException("Cannot find task with name " + taskName);
  }
}
