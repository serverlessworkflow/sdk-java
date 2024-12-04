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
import io.serverlessworkflow.impl.StringFilter;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class RaiseExecutor extends AbstractTaskExecutor<RaiseTask> {

  private final BiFunction<WorkflowContext, TaskContext<RaiseTask>, WorkflowError> errorBuilder;

  private final StringFilter typeFilter;
  private final Optional<StringFilter> instanceFilter;
  private final StringFilter titleFilter;
  private final StringFilter detailFilter;

  protected RaiseExecutor(RaiseTask task, WorkflowDefinition definition) {
    super(task, definition);
    RaiseTaskError raiseError = task.getRaise().getError();
    Error error =
        raiseError.getRaiseErrorDefinition() != null
            ? raiseError.getRaiseErrorDefinition()
            : findError(definition, raiseError.getRaiseErrorReference());
    this.typeFilter = getTypeFunction(definition.expressionFactory(), error.getType());
    this.instanceFilter = getInstanceFunction(definition.expressionFactory(), error.getInstance());
    this.titleFilter =
        WorkflowUtils.buildStringFilter(definition.expressionFactory(), error.getTitle());
    this.detailFilter =
        WorkflowUtils.buildStringFilter(definition.expressionFactory(), error.getDetail());
    this.errorBuilder = (w, t) -> buildError(error, w, t);
  }

  private static Error findError(WorkflowDefinition definition, String raiseErrorReference) {
    Map<String, Error> errorsMap =
        definition.workflow().getUse().getErrors().getAdditionalProperties();
    Error error = errorsMap.get(raiseErrorReference);
    if (error == null) {
      throw new IllegalArgumentException("Error " + error + "is not defined in " + errorsMap);
    }
    return error;
  }

  private WorkflowError buildError(
      Error error, WorkflowContext context, TaskContext<RaiseTask> taskContext) {
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
      ExpressionFactory expressionFactory, ErrorInstance errorInstance) {
    return errorInstance != null
        ? Optional.of(
            WorkflowUtils.buildStringFilter(
                expressionFactory,
                errorInstance.getExpressionErrorInstance(),
                errorInstance.getLiteralErrorInstance()))
        : Optional.empty();
  }

  private StringFilter getTypeFunction(ExpressionFactory expressionFactory, ErrorType type) {
    return WorkflowUtils.buildStringFilter(
        expressionFactory,
        type.getExpressionErrorType(),
        type.getLiteralErrorType().get().toString());
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<RaiseTask> taskContext) {
    throw new WorkflowException(errorBuilder.apply(workflow, taskContext));
  }
}
