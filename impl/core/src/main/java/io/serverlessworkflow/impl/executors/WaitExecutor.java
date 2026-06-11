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
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WaitExecutor extends RegularTaskExecutor<WaitTask> {

  private final Duration duration;
  private final WorkflowFilter durationExpressionFilter;

  public static class WaitExecutorBuilder extends RegularTaskExecutorBuilder<WaitTask> {
    private Duration duration = null;
    private WorkflowFilter durationExpressionFilter;

    protected WaitExecutorBuilder(
        WorkflowMutablePosition position, WaitTask task, WorkflowDefinition definition) {
      super(position, task, definition);
      if (task.getWait().getDurationExpression() == null) {
        this.duration =
            task.getWait().getDurationInline() != null
                ? toDuration(task.getWait().getDurationInline())
                : parseDurationLiteral(task.getWait().getDurationLiteral());
      } else {
        this.durationExpressionFilter =
            WorkflowUtils.buildWorkflowFilter(application, task.getWait().getDurationExpression());
      }
    }

    private Duration toDuration(DurationInline durationInline) {
      return Duration.ofMillis(durationInline.getMilliseconds())
          .plusSeconds(durationInline.getSeconds())
          .plusMinutes(durationInline.getMinutes())
          .plusHours(durationInline.getHours())
          .plusDays(durationInline.getDays());
    }

    private Duration parseDurationLiteral(String literal) {
      if (!WorkflowUtils.isValid(literal)) {
        throw new IllegalArgumentException(
            "Wait task duration literal cannot be null or empty at position: "
                + position.jsonPointer());
      }
      try {
        return Duration.parse(literal);
      } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid ISO 8601 duration literal '"
                + literal
                + "' at position: "
                + position.jsonPointer(),
            e);
      }
    }

    @Override
    public WaitExecutor buildInstance() {
      return new WaitExecutor(this);
    }
  }

  protected WaitExecutor(WaitExecutorBuilder builder) {
    super(builder);
    this.duration = builder.duration;
    this.durationExpressionFilter = builder.durationExpressionFilter;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    workflow.instance().status(WorkflowStatus.WAITING);
    return new CompletableFuture<WorkflowModel>()
        .completeOnTimeout(
            taskContext.output(),
            Objects.requireNonNullElseGet(
                    duration, () -> evaluateDurationExpression(workflow, taskContext))
                .toMillis(),
            TimeUnit.MILLISECONDS);
  }

  private Duration evaluateDurationExpression(WorkflowContext workflow, TaskContext taskContext) {
    String durationString =
        durationExpressionFilter
            .apply(workflow, taskContext, taskContext.rawInput())
            .as(String.class)
            .orElse(null);

    if (!WorkflowUtils.isValid(durationString)) {
      throw new IllegalArgumentException(
          "Wait duration expression evaluated to empty or null at task: "
              + taskContext.position().jsonPointer()
              + ". Expression must return a valid ISO 8601 duration string.");
    }

    try {
      return Duration.parse(durationString.trim());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Wait duration expression returned invalid ISO 8601 duration '"
              + durationString
              + "' at task: "
              + taskContext.position().jsonPointer()
              + ". Expected format: PT[n]H[n]M[n]S (e.g., PT1H30M, PT5S)",
          e);
    }
  }
}
