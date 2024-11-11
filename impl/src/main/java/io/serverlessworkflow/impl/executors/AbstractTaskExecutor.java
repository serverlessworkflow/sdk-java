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
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTaskExecutor<T extends TaskBase> implements TaskExecutor<T> {

  protected final T task;
  protected final ExpressionFactory exprFactory;

  private interface TaskFilter<V extends TaskBase> {
    JsonNode apply(WorkflowContext workflow, TaskContext<V> task, JsonNode node);
  }

  private final Optional<TaskFilter<T>> inputProcessor;
  private final Optional<TaskFilter<T>> outputProcessor;
  private final Optional<TaskFilter<T>> contextProcessor;

  protected AbstractTaskExecutor(T task, ExpressionFactory exprFactory) {
    this.task = task;
    this.exprFactory = exprFactory;
    this.inputProcessor = Optional.ofNullable(getInputProcessor());
    this.outputProcessor = Optional.ofNullable(getOutputProcessor());
    this.contextProcessor = Optional.ofNullable(getContextProcessor());
  }

  private TaskFilter<T> getInputProcessor() {
    if (task.getInput() != null) {
      Input input = task.getInput();
      // TODO add schema validator
      if (input.getFrom() != null) {
        return getTaskFilter(input.getFrom().getString(), input.getFrom().getObject());
      }
    }
    return null;
  }

  private TaskFilter<T> getOutputProcessor() {
    if (task.getOutput() != null) {
      Output output = task.getOutput();
      // TODO add schema validator
      if (output.getAs() != null) {
        return getTaskFilter(output.getAs().getString(), output.getAs().getObject());
      }
    }
    return null;
  }

  private TaskFilter<T> getContextProcessor() {
    if (task.getExport() != null) {
      Export export = task.getExport();
      // TODO add schema validator
      if (export.getAs() != null) {
        return getTaskFilter(export.getAs().getString(), export.getAs().getObject());
      }
    }
    return null;
  }

  private TaskFilter<T> getTaskFilter(String str, Object object) {
    if (str != null) {
      Expression expression = exprFactory.getExpression(str);
      return expression::eval;
    } else {
      Object exprObj = ExpressionUtils.buildExpressionObject(object, exprFactory);
      return exprObj instanceof Map
          ? (w, t, n) ->
              JsonUtils.fromValue(
                  ExpressionUtils.evaluateExpressionMap((Map<String, Object>) exprObj, w, t, n))
          : (w, t, n) -> JsonUtils.fromValue(object);
    }
  }

  @Override
  public JsonNode apply(WorkflowContext workflowContext, JsonNode rawInput) {
    TaskContext<T> taskContext = new TaskContext<>(rawInput, task);
    inputProcessor.ifPresent(
        p -> taskContext.input(p.apply(workflowContext, taskContext, taskContext.rawInput())));
    taskContext.rawOutput(internalExecute(workflowContext, taskContext, taskContext.input()));
    outputProcessor.ifPresent(
        p -> taskContext.output(p.apply(workflowContext, taskContext, taskContext.rawOutput())));
    contextProcessor.ifPresent(
        p ->
            workflowContext.context(
                p.apply(workflowContext, taskContext, workflowContext.context())));
    return taskContext.output();
  }

  protected abstract JsonNode internalExecute(
      WorkflowContext workflow, TaskContext<T> task, JsonNode node);
}
