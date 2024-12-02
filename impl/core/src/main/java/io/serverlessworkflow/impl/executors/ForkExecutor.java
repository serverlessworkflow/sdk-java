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

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowState;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.generic.SortedArrayList;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForkExecutor extends AbstractTaskExecutor<ForkTask> {

  private static final Logger logger = LoggerFactory.getLogger(ForkExecutor.class);
  private final ExecutorService service;

  protected ForkExecutor(ForkTask task, WorkflowDefinition definition) {
    super(task, definition);
    service = definition.executorService();
  }

  private record BranchContext(String taskName, TaskContext<?> taskContext) {}

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<ForkTask> taskContext) {
    ForkTaskConfiguration forkConfig = task.getFork();

    if (!forkConfig.getBranches().isEmpty()) {
      Map<String, Future<TaskContext<?>>> futures = new HashMap<>();
      int index = 0;
      for (TaskItem item : forkConfig.getBranches()) {
        final int i = index++;
        futures.put(
            item.getName(),
            service.submit(() -> executeBranch(workflow, taskContext.copy(), item, i)));
      }
      List<BranchContext> results =
          new SortedArrayList<>(
              (arg1, arg2) ->
                  arg1.taskContext.completedAt().compareTo(arg2.taskContext.completedAt()));
      for (Map.Entry<String, Future<TaskContext<?>>> entry : futures.entrySet()) {
        try {
          results.add(new BranchContext(entry.getKey(), entry.getValue().get()));
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          } else {
            throw new UndeclaredThrowableException(ex);
          }
        } catch (InterruptedException ex) {
          logger.warn(
              "Thred executing branch {} was interrupted, this branch will be ignored",
              entry.getKey(),
              ex);
        }
      }
      if (!results.isEmpty()) {
        taskContext.rawOutput(
            forkConfig.isCompete()
                ? results.get(0).taskContext().output()
                : JsonUtils.fromValue(
                    results.stream()
                        .map(
                            e ->
                                JsonUtils.mapper()
                                    .createObjectNode()
                                    .set(e.taskName(), e.taskContext().output()))
                        .collect(Collectors.toList())));
      }
    }
  }

  private TaskContext<?> executeBranch(
      WorkflowContext workflow, TaskContext<ForkTask> taskContext, TaskItem taskItem, int index) {
    taskContext.position().addIndex(index);
    TaskContext<?> result =
        WorkflowUtils.executeTask(workflow, taskContext, taskItem, taskContext.input());
    if (result.flowDirective() != null
        && result.flowDirective().getFlowDirectiveEnum() == FlowDirectiveEnum.END) {
      workflow.instance().state(WorkflowState.COMPLETED);
    }
    taskContext.position().back();
    return result;
  }
}
