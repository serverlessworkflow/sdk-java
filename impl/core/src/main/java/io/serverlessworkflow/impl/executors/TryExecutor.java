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

import io.serverlessworkflow.api.types.CatchErrors;
import io.serverlessworkflow.api.types.ErrorFilter;
import io.serverlessworkflow.api.types.Retry;
import io.serverlessworkflow.api.types.RetryBackoff;
import io.serverlessworkflow.api.types.RetryPolicy;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.executors.retry.ConstantRetryIntervalFunction;
import io.serverlessworkflow.impl.executors.retry.DefaultRetryExecutor;
import io.serverlessworkflow.impl.executors.retry.ExponentialRetryIntervalFunction;
import io.serverlessworkflow.impl.executors.retry.LinearRetryIntervalFunction;
import io.serverlessworkflow.impl.executors.retry.RetryExecutor;
import io.serverlessworkflow.impl.executors.retry.RetryIntervalFunction;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

public class TryExecutor extends RegularTaskExecutor<TryTask> {

  private final Optional<WorkflowPredicate> whenFilter;
  private final Optional<WorkflowPredicate> exceptFilter;
  private final Optional<Predicate<WorkflowError>> errorFilter;
  private final TaskExecutor<?> taskExecutor;
  private final Optional<TaskExecutor<?>> catchTaskExecutor;
  private final Optional<RetryExecutor> retryIntervalExecutor;

  public static class TryExecutorBuilder extends RegularTaskExecutorBuilder<TryTask> {

    private final Optional<WorkflowPredicate> whenFilter;
    private final Optional<WorkflowPredicate> exceptFilter;
    private final Optional<Predicate<WorkflowError>> errorFilter;
    private final TaskExecutor<?> taskExecutor;
    private final Optional<TaskExecutor<?>> catchTaskExecutor;
    private final Optional<RetryExecutor> retryIntervalExecutor;

    protected TryExecutorBuilder(
        WorkflowMutablePosition position, TryTask task, WorkflowDefinition definition) {
      super(position, task, definition);
      TryTaskCatch catchInfo = task.getCatch();
      this.errorFilter = buildErrorFilter(catchInfo.getErrors());
      this.whenFilter = WorkflowUtils.optionalPredicate(application, catchInfo.getWhen());
      this.exceptFilter = WorkflowUtils.optionalPredicate(application, catchInfo.getExceptWhen());
      this.taskExecutor =
          TaskExecutorHelper.createExecutorList(position, task.getTry(), definition);
      TryTaskCatch catchTask = task.getCatch();
      if (catchTask != null) {
        List<TaskItem> catchTaskDo = catchTask.getDo();

        this.catchTaskExecutor =
            catchTaskDo != null && !catchTaskDo.isEmpty()
                ? Optional.of(
                    TaskExecutorHelper.createExecutorList(position, catchTaskDo, definition))
                : Optional.empty();

        Retry retry = catchTask.getRetry();
        this.retryIntervalExecutor = retry != null ? buildRetryInterval(retry) : Optional.empty();
      } else {
        this.catchTaskExecutor = Optional.empty();
        this.retryIntervalExecutor = Optional.empty();
      }
    }

    private Optional<RetryExecutor> buildRetryInterval(Retry retry) {
      RetryPolicy retryPolicy = null;
      if (retry.getRetryPolicyDefinition() != null) {
        retryPolicy = retry.getRetryPolicyDefinition();
      } else if (retry.getRetryPolicyReference() != null) {
        retryPolicy =
            workflow
                .getUse()
                .getRetries()
                .getAdditionalProperties()
                .get(retry.getRetryPolicyReference());
        if (retryPolicy == null) {
          throw new IllegalStateException("Retry policy " + retryPolicy + " was not found");
        }
      }
      return retryPolicy != null ? Optional.of(buildRetryExecutor(retryPolicy)) : Optional.empty();
    }

    protected RetryExecutor buildRetryExecutor(RetryPolicy retryPolicy) {
      return new DefaultRetryExecutor(
          retryPolicy.getLimit().getAttempt().getCount(),
          buildIntervalFunction(retryPolicy),
          WorkflowUtils.optionalPredicate(application, retryPolicy.getWhen()),
          WorkflowUtils.optionalPredicate(application, retryPolicy.getExceptWhen()));
    }

    private RetryIntervalFunction buildIntervalFunction(RetryPolicy retryPolicy) {
      RetryBackoff backoff = retryPolicy.getBackoff();
      if (backoff.getConstantBackoff() != null) {
        return new ConstantRetryIntervalFunction(
            application, retryPolicy.getDelay(), retryPolicy.getJitter());
      } else if (backoff.getLinearBackoff() != null) {
        return new LinearRetryIntervalFunction(
            application, retryPolicy.getDelay(), retryPolicy.getJitter());
      } else if (backoff.getExponentialBackOff() != null) {
        return new ExponentialRetryIntervalFunction(
            application, retryPolicy.getDelay(), retryPolicy.getJitter());
      }
      throw new IllegalStateException("A backoff strategy should be set");
    }

    @Override
    public TryExecutor buildInstance() {
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
    this.retryIntervalExecutor = builder.retryIntervalExecutor;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return doIt(workflow, taskContext, taskContext.input());
  }

  private CompletableFuture<WorkflowModel> doIt(
      WorkflowContext workflow, TaskContext taskContext, WorkflowModel model) {
    return TaskExecutorHelper.processTaskList(
            taskExecutor, workflow, Optional.of(taskContext), model)
        .exceptionallyCompose(e -> handleException(e, workflow, taskContext));
  }

  private CompletableFuture<WorkflowModel> handleException(
      Throwable e, WorkflowContext workflow, TaskContext taskContext) {
    if (e instanceof CompletionException) {
      return handleException(e.getCause(), workflow, taskContext);
    }
    if (e instanceof WorkflowException) {
      WorkflowException exception = (WorkflowException) e;
      CompletableFuture<WorkflowModel> completable =
          CompletableFuture.completedFuture(taskContext.rawOutput());
      if (errorFilter.map(f -> f.test(exception.getWorkflowError())).orElse(true)
          && WorkflowUtils.whenExceptTest(
              whenFilter, exceptFilter, workflow, taskContext, taskContext.rawOutput())) {
        if (catchTaskExecutor.isPresent()) {
          completable =
              completable.thenCompose(
                  model ->
                      TaskExecutorHelper.processTaskList(
                          catchTaskExecutor.get(), workflow, Optional.of(taskContext), model));
        }
        if (retryIntervalExecutor.isPresent()) {
          completable =
              completable
                  .thenCompose(
                      model ->
                          retryIntervalExecutor
                              .get()
                              .retry(workflow, taskContext, model)
                              .orElse(CompletableFuture.failedFuture(e)))
                  .thenCompose(model -> doIt(workflow, taskContext, model));
        }
      }
      return completable;
    } else {
      return CompletableFuture.failedFuture(e);
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
