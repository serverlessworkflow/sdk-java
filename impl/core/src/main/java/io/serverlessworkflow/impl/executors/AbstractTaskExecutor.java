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
import io.serverlessworkflow.impl.WorkflowDefinition;
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

  protected AbstractTaskExecutor(T task, WorkflowDefinition definition) {
    this.task = task;
    buildInputProcessors(definition);
    buildOutputProcessors(definition);
    buildContextProcessors(definition);
  }

  private void buildInputProcessors(WorkflowDefinition definition) {
    if (task.getInput() != null) {
      Input input = task.getInput();
      this.inputProcessor = buildWorkflowFilter(definition.expressionFactory(), input.getFrom());
      this.inputSchemaValidator =
          getSchemaValidator(
              definition.validatorFactory(),
              schemaToNode(definition.resourceLoader(), input.getSchema()));
    }
  }

  private void buildOutputProcessors(WorkflowDefinition definition) {
    if (task.getOutput() != null) {
      Output output = task.getOutput();
      this.outputProcessor = buildWorkflowFilter(definition.expressionFactory(), output.getAs());
      this.outputSchemaValidator =
          getSchemaValidator(
              definition.validatorFactory(),
              schemaToNode(definition.resourceLoader(), output.getSchema()));
    }
  }

  private void buildContextProcessors(WorkflowDefinition definition) {
    if (task.getExport() != null) {
      Export export = task.getExport();
      if (export.getAs() != null) {
        this.contextProcessor = buildWorkflowFilter(definition.expressionFactory(), export.getAs());
      }
      this.contextSchemaValidator =
          getSchemaValidator(
              definition.validatorFactory(),
              schemaToNode(definition.resourceLoader(), export.getSchema()));
    }
  }

  @Override
  public TaskContext<T> apply(WorkflowContext workflowContext, JsonNode rawInput) {
    TaskContext<T> taskContext = new TaskContext<>(rawInput, task);
    inputSchemaValidator.ifPresent(s -> s.validate(taskContext.rawInput()));
    inputProcessor.ifPresent(
        p ->
            taskContext.input(
                p.apply(workflowContext, Optional.of(taskContext), taskContext.rawInput())));
    internalExecute(workflowContext, taskContext);
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
    return taskContext;
  }

  protected abstract void internalExecute(WorkflowContext workflow, TaskContext<T> taskContext);
}
