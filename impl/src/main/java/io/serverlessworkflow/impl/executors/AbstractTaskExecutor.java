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

import static io.serverlessworkflow.impl.WorkflowUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFactories;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import java.util.Optional;

public abstract class AbstractTaskExecutor<T extends TaskBase> implements TaskExecutor<T> {

  protected final T task;

  private Optional<WorkflowFilter> inputProcessor = Optional.empty();
  private Optional<WorkflowFilter> outputProcessor = Optional.empty();
  private Optional<WorkflowFilter> contextProcessor = Optional.empty();
  private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> contextSchemaValidator = Optional.empty();

  protected AbstractTaskExecutor(T task, WorkflowFactories holder) {
    this.task = task;
    buildInputProcessors(holder);
    buildOutputProcessors(holder);
    buildContextProcessors(holder);
  }

  private void buildInputProcessors(WorkflowFactories holder) {
    if (task.getInput() != null) {
      Input input = task.getInput();
      this.inputProcessor = buildWorkflowFilter(holder.getExpressionFactory(), input.getFrom());
      this.inputSchemaValidator =
          getSchemaValidator(holder.getValidatorFactory(), schemaToNode(holder, input.getSchema()));
    }
  }

  private void buildOutputProcessors(WorkflowFactories holder) {
    if (task.getOutput() != null) {
      Output output = task.getOutput();
      this.outputProcessor = buildWorkflowFilter(holder.getExpressionFactory(), output.getAs());
      this.outputSchemaValidator =
          getSchemaValidator(
              holder.getValidatorFactory(), schemaToNode(holder, output.getSchema()));
    }
  }

  private void buildContextProcessors(WorkflowFactories holder) {
    if (task.getExport() != null) {
      Export export = task.getExport();
      if (export.getAs() != null) {
        this.contextProcessor = buildWorkflowFilter(holder.getExpressionFactory(), export.getAs());
      }
      this.contextSchemaValidator =
          getSchemaValidator(
              holder.getValidatorFactory(), schemaToNode(holder, export.getSchema()));
    }
  }

  @Override
  public JsonNode apply(WorkflowContext workflowContext, JsonNode rawInput) {
    TaskContext<T> taskContext = new TaskContext<>(rawInput, task);
    inputSchemaValidator.ifPresent(s -> s.validate(taskContext.rawInput()));
    inputProcessor.ifPresent(
        p ->
            taskContext.input(
                p.apply(workflowContext, Optional.of(taskContext), taskContext.rawInput())));
    taskContext.rawOutput(internalExecute(workflowContext, taskContext, taskContext.input()));
    outputProcessor.ifPresent(
        p ->
            taskContext.output(
                p.apply(workflowContext, Optional.of(taskContext), taskContext.rawOutput())));
    outputSchemaValidator.ifPresent(s -> s.validate(taskContext.output()));
    contextProcessor.ifPresent(
        p ->
            workflowContext.context(
                p.apply(workflowContext, Optional.of(taskContext), workflowContext.context())));
    contextSchemaValidator.ifPresent(s -> s.validate(workflowContext.context()));
    return taskContext.output();
  }

  protected abstract JsonNode internalExecute(
      WorkflowContext workflow, TaskContext<T> task, JsonNode node);
}
