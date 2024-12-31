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
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.executors.RegularTaskExecutor.RegularTaskExecutorBuilder;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ForExecutor extends RegularTaskExecutor<ForTask> {

  private final WorkflowFilter collectionExpr;
  private final Optional<WorkflowFilter> whileExpr;
  private final TaskExecutor<?> taskExecutor;

  public static class ForExecutorBuilder extends RegularTaskExecutorBuilder<ForTask> {
    private WorkflowFilter collectionExpr;
    private Optional<WorkflowFilter> whileExpr;
    private TaskExecutor<?> taskExecutor;

    protected ForExecutorBuilder(
        WorkflowPosition position,
        ForTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      ForTaskConfiguration forConfig = task.getFor();
      this.collectionExpr =
          WorkflowUtils.buildWorkflowFilter(application.expressionFactory(), forConfig.getIn());
      this.whileExpr =
          WorkflowUtils.optionalFilter(application.expressionFactory(), task.getWhile());
      this.taskExecutor =
          TaskExecutorHelper.createExecutorList(
              position, task.getDo(), workflow, application, resourceLoader);
    }

    @Override
    public TaskExecutor<ForTask> buildInstance() {
      return new ForExecutor(this);
    }
  }

  protected ForExecutor(ForExecutorBuilder builder) {
    super(builder);
    this.collectionExpr = builder.collectionExpr;
    this.whileExpr = builder.whileExpr;
    this.taskExecutor = builder.taskExecutor;
  }

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    Iterator<JsonNode> iter =
        collectionExpr.apply(workflow, taskContext, taskContext.input()).iterator();
    int i = 0;
    CompletableFuture<JsonNode> future = CompletableFuture.completedFuture(taskContext.input());
    while (iter.hasNext()
        && whileExpr
            .<JsonNode>map(w -> w.apply(workflow, taskContext, taskContext.rawOutput()))
            .map(n -> n.asBoolean(true))
            .orElse(true)) {
      JsonNode item = iter.next();
      taskContext.variables().put(task.getFor().getEach(), item);
      taskContext.variables().put(task.getFor().getAt(), i++);
      future =
          future.thenCompose(
              input ->
                  TaskExecutorHelper.processTaskList(
                      taskExecutor, workflow, Optional.of(taskContext), input));
    }
    return future;
  }
}
