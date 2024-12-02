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
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Optional;
import java.util.function.Predicate;

public class TryExecutor extends AbstractTaskExecutor<TryTask> {

  private final Optional<WorkflowFilter> whenFilter;
  private final Optional<WorkflowFilter> exceptFilter;
  private final Optional<Predicate<WorkflowError>> errorFilter;

  protected TryExecutor(TryTask task, WorkflowDefinition definition) {
    super(task, definition);
    TryTaskCatch catchInfo = task.getCatch();
    this.errorFilter = buildErrorFilter(catchInfo.getErrors());
    this.whenFilter =
        WorkflowUtils.optionalFilter(definition.expressionFactory(), catchInfo.getWhen());
    this.exceptFilter =
        WorkflowUtils.optionalFilter(definition.expressionFactory(), catchInfo.getExceptWhen());
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<TryTask> taskContext) {
    try {
      WorkflowUtils.processTaskList(task.getTry(), workflow, taskContext);
    } catch (WorkflowException exception) {
      if (errorFilter.map(f -> f.test(exception.getWorflowError())).orElse(true)
          && whenFilter
              .map(w -> w.apply(workflow, taskContext, taskContext.input()).asBoolean())
              .orElse(true)
          && exceptFilter
              .map(w -> !w.apply(workflow, taskContext, taskContext.input()).asBoolean())
              .orElse(true)) {
        if (task.getCatch().getDo() != null) {
          WorkflowUtils.processTaskList(task.getCatch().getDo(), workflow, taskContext);
        }
      } else {
        throw exception;
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
