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
import io.serverlessworkflow.api.types.CatchErrors;
import io.serverlessworkflow.api.types.ErrorFilter;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

public class TryExecutor extends RegularTaskExecutor<TryTask> {

  private final Optional<WorkflowFilter> whenFilter;
  private final Optional<WorkflowFilter> exceptFilter;
  private final Optional<Predicate<WorkflowError>> errorFilter;
  private final TaskExecutor<?> taskExecutor;
  private final Optional<TaskExecutor<?>> catchTaskExecutor;

  public static class TryExecutorBuilder extends RegularTaskExecutorBuilder<TryTask> {

    private final Optional<WorkflowFilter> whenFilter;
    private final Optional<WorkflowFilter> exceptFilter;
    private final Optional<Predicate<WorkflowError>> errorFilter;
    private final TaskExecutor<?> taskExecutor;
    private final Optional<TaskExecutor<?>> catchTaskExecutor;

    protected TryExecutorBuilder(
        WorkflowPosition position,
        TryTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      TryTaskCatch catchInfo = task.getCatch();
      this.errorFilter = buildErrorFilter(catchInfo.getErrors());
      this.whenFilter =
          WorkflowUtils.optionalFilter(application.expressionFactory(), catchInfo.getWhen());
      this.exceptFilter =
          WorkflowUtils.optionalFilter(application.expressionFactory(), catchInfo.getExceptWhen());
      this.taskExecutor =
          TaskExecutorHelper.createExecutorList(
              position, task.getTry(), workflow, application, resourceLoader);
      List<TaskItem> catchTask = task.getCatch().getDo();
      this.catchTaskExecutor =
          catchTask != null && !catchTask.isEmpty()
              ? Optional.of(
                  TaskExecutorHelper.createExecutorList(
                      position, task.getCatch().getDo(), workflow, application, resourceLoader))
              : Optional.empty();
    }

    @Override
    public TaskExecutor<TryTask> buildInstance() {
      return new TryExecutor(this);
    }
  }

  protected TryExecutor(TryExecutorBuilder builder) {
    super(builder);
    this.errorFilter = builder.errorFilter;
    this.whenFilter = builder.whenFilter;
    this.exceptFilter = builder.exceptFilter;
    this.taskExecutor = builder.taskExecutor;
    this.catchTaskExecutor = builder.catchTaskExecutor;
  }

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return TaskExecutorHelper.processTaskList(
            taskExecutor, workflow, Optional.of(taskContext), taskContext.input())
        .exceptionallyCompose(e -> handleException(e, workflow, taskContext));
  }

  private CompletableFuture<JsonNode> handleException(
      Throwable e, WorkflowContext workflow, TaskContext taskContext) {
    if (e instanceof CompletionException) {
      return handleException(e.getCause(), workflow, taskContext);
    }
    if (e instanceof WorkflowException) {
      WorkflowException exception = (WorkflowException) e;
      if (errorFilter.map(f -> f.test(exception.getWorflowError())).orElse(true)
          && whenFilter
              .map(w -> w.apply(workflow, taskContext, taskContext.input()).asBoolean())
              .orElse(true)
          && exceptFilter
              .map(w -> !w.apply(workflow, taskContext, taskContext.input()).asBoolean())
              .orElse(true)) {
        if (catchTaskExecutor.isPresent()) {
          return TaskExecutorHelper.processTaskList(
              catchTaskExecutor.get(), workflow, Optional.of(taskContext), taskContext.input());
        }
      }
      return CompletableFuture.completedFuture(taskContext.rawOutput());
    } else {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  private static Optional<Predicate<WorkflowError>> buildErrorFilter(CatchErrors errors) {
    return errors != null
        ? Optional.of(error -> filterError(error, errors.getWith()))
        : Optional.empty();
  }

  private static boolean filterError(WorkflowError error, ErrorFilter errorFilter) {
    return compareString(errorFilter.getType(), error.type())
        && (errorFilter.getStatus() <= 0 || error.status() == errorFilter.getStatus())
        && compareString(errorFilter.getInstance(), error.instance())
        && compareString(errorFilter.getTitle(), error.title())
        && compareString(errorFilter.getDetails(), errorFilter.getDetails());
  }

  private static boolean compareString(String one, String other) {
    return one == null || one.equals(other);
  }
}
