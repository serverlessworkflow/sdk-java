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
import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.executors.RegularTaskExecutor.RegularTaskExecutorBuilder;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WaitExecutor extends RegularTaskExecutor<WaitTask> {

  private final Duration millisToWait;

  public static class WaitExecutorBuilder extends RegularTaskExecutorBuilder<WaitTask> {
    private final Duration millisToWait;

    protected WaitExecutorBuilder(
        WorkflowPosition position,
        WaitTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
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
    public TaskExecutor<WaitTask> buildInstance() {
      return new WaitExecutor(this);
    }
  }

  protected WaitExecutor(WaitExecutorBuilder builder) {
    super(builder);
    this.millisToWait = builder.millisToWait;
  }

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return new CompletableFuture<JsonNode>()
        .completeOnTimeout(taskContext.output(), millisToWait.toMillis(), TimeUnit.MILLISECONDS);
  }
}
