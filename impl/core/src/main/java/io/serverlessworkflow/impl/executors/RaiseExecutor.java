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

import io.serverlessworkflow.api.types.Error;
import io.serverlessworkflow.api.types.ErrorInstance;
import io.serverlessworkflow.api.types.ErrorType;
import io.serverlessworkflow.api.types.RaiseTask;
import io.serverlessworkflow.api.types.RaiseTaskError;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.StringFilter;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class RaiseExecutor extends RegularTaskExecutor<RaiseTask> {

  private final BiFunction<WorkflowContext, TaskContext, WorkflowError> errorBuilder;

  public static class RaiseExecutorBuilder extends RegularTaskExecutorBuilder<RaiseTask> {

    private final BiFunction<WorkflowContext, TaskContext, WorkflowError> errorBuilder;
    private final StringFilter typeFilter;
    private final Optional<StringFilter> instanceFilter;
    private final StringFilter titleFilter;
    private final StringFilter detailFilter;

    protected RaiseExecutorBuilder(
        WorkflowPosition position,
        RaiseTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      RaiseTaskError raiseError = task.getRaise().getError();
      Error error =
          raiseError.getRaiseErrorDefinition() != null
              ? raiseError.getRaiseErrorDefinition()
              : findError(raiseError.getRaiseErrorReference());
      this.typeFilter = getTypeFunction(application, error.getType());
      this.instanceFilter = getInstanceFunction(application, error.getInstance());
      this.titleFilter =
          WorkflowUtils.buildStringFilter(
              application,
              error.getTitle().getExpressionErrorTitle(),
              error.getTitle().getLiteralErrorTitle());
      this.detailFilter =
          WorkflowUtils.buildStringFilter(
              application,
              error.getDetail().getExpressionErrorDetails(),
              error.getTitle().getExpressionErrorTitle());
      this.errorBuilder = (w, t) -> buildError(error, w, t);
    }

    private WorkflowError buildError(
        Error error, WorkflowContext context, TaskContext taskContext) {
      return WorkflowError.error(typeFilter.apply(context, taskContext), error.getStatus())
          .instance(
              instanceFilter
                  .map(f -> f.apply(context, taskContext))
                  .orElseGet(() -> taskContext.position().jsonPointer()))
          .title(titleFilter.apply(context, taskContext))
          .details(detailFilter.apply(context, taskContext))
          .build();
    }

    private Optional<StringFilter> getInstanceFunction(
        WorkflowApplication app, ErrorInstance errorInstance) {
      return errorInstance != null
          ? Optional.of(
              WorkflowUtils.buildStringFilter(
                  app,
                  errorInstance.getExpressionErrorInstance(),
                  errorInstance.getLiteralErrorInstance()))
          : Optional.empty();
    }

    private StringFilter getTypeFunction(WorkflowApplication app, ErrorType type) {
      return WorkflowUtils.buildStringFilter(
          app, type.getExpressionErrorType(), type.getLiteralErrorType().get().toString());
    }

    private Error findError(String raiseErrorReference) {
      Map<String, Error> errorsMap = workflow.getUse().getErrors().getAdditionalProperties();
      Error error = errorsMap.get(raiseErrorReference);
      if (error == null) {
        throw new IllegalArgumentException("Error " + error + "is not defined in " + errorsMap);
      }
      return error;
    }

    @Override
    public TaskExecutor<RaiseTask> buildInstance() {
      return new RaiseExecutor(this);
    }
  }

  protected RaiseExecutor(RaiseExecutorBuilder builder) {
    super(builder);
    this.errorBuilder = builder.errorBuilder;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    throw new WorkflowException(errorBuilder.apply(workflow, taskContext));
  }
}
