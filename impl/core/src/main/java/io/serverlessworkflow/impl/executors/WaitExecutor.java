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

import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutableInstance;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WaitExecutor extends RegularTaskExecutor<WaitTask> {

  private final Duration millisToWait;

  public static class WaitExecutorBuilder extends RegularTaskExecutorBuilder<WaitTask> {
    private final Duration millisToWait;

    protected WaitExecutorBuilder(
        WorkflowMutablePosition position, WaitTask task, WorkflowDefinition definition) {
      super(position, task, definition);
      this.millisToWait =
          task.getWait().getDurationInline() != null
              ? toLong(task.getWait().getDurationInline())
              : Duration.parse(task.getWait().getDurationExpression());
    }

    private Duration toLong(DurationInline durationInline) {
      Duration duration = Duration.ofMillis(durationInline.getMilliseconds());
      duration.plus(Duration.ofSeconds(durationInline.getSeconds()));
      duration.plus(Duration.ofMinutes(durationInline.getMinutes()));
      duration.plus(Duration.ofHours(durationInline.getHours()));
      duration.plus(Duration.ofDays(durationInline.getDays()));
      return duration;
    }

    @Override
    public WaitExecutor buildInstance() {
      return new WaitExecutor(this);
    }
  }

  protected WaitExecutor(WaitExecutorBuilder builder) {
    super(builder);
    this.millisToWait = builder.millisToWait;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    ((WorkflowMutableInstance) workflow.instance()).status(WorkflowStatus.WAITING);
    return new CompletableFuture<WorkflowModel>()
        .completeOnTimeout(taskContext.output(), millisToWait.toMillis(), TimeUnit.MILLISECONDS)
        .thenApply(this::complete);
  }

  private WorkflowModel complete(WorkflowModel model) {
    return model;
  }
}
