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
package io.serverlessworkflow.impl;

import static io.serverlessworkflow.impl.WorkflowUtils.*;

import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowDefinition implements AutoCloseable {

  private final Workflow workflow;
  private final Collection<WorkflowExecutionListener> listeners;
  private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
  private Optional<WorkflowFilter> inputFilter = Optional.empty();
  private Optional<WorkflowFilter> outputFilter = Optional.empty();
  private final TaskExecutorFactory taskFactory;
  private final ExpressionFactory exprFactory;
  private final ResourceLoader resourceLoader;
  private final SchemaValidatorFactory schemaValidatorFactory;
  private final WorkflowPositionFactory positionFactory;
  private final Map<String, TaskExecutor<? extends TaskBase>> taskExecutors =
      new ConcurrentHashMap<>();

  private WorkflowDefinition(
      Workflow workflow,
      Collection<WorkflowExecutionListener> listeners,
      TaskExecutorFactory taskFactory,
      ResourceLoader resourceLoader,
      ExpressionFactory exprFactory,
      SchemaValidatorFactory schemaValidatorFactory,
      WorkflowPositionFactory positionFactory) {
    this.workflow = workflow;
    this.listeners = listeners;
    this.taskFactory = taskFactory;
    this.exprFactory = exprFactory;
    this.schemaValidatorFactory = schemaValidatorFactory;
    this.positionFactory = positionFactory;
    this.resourceLoader = resourceLoader;
    if (workflow.getInput() != null) {
      Input input = workflow.getInput();
      this.inputSchemaValidator =
          getSchemaValidator(
              schemaValidatorFactory, schemaToNode(resourceLoader, input.getSchema()));
      this.inputFilter = buildWorkflowFilter(exprFactory, input.getFrom());
    }
    if (workflow.getOutput() != null) {
      Output output = workflow.getOutput();
      this.outputSchemaValidator =
          getSchemaValidator(
              schemaValidatorFactory, schemaToNode(resourceLoader, output.getSchema()));
      this.outputFilter = buildWorkflowFilter(exprFactory, output.getAs());
    }
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow) {
    return of(application, workflow, null);
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow, Path path) {
    return new WorkflowDefinition(
        workflow,
        application.listeners(),
        application.taskFactory(),
        application.resourceLoaderFactory().getResourceLoader(path),
        application.expressionFactory(),
        application.validatorFactory(),
        application.positionFactory());
  }

  public WorkflowInstance execute(Object input) {
    return new WorkflowInstance(this, JsonUtils.fromValue(input));
  }

  public Optional<SchemaValidator> inputSchemaValidator() {
    return inputSchemaValidator;
  }

  public Optional<WorkflowFilter> inputFilter() {
    return inputFilter;
  }

  public Workflow workflow() {
    return workflow;
  }

  public Collection<WorkflowExecutionListener> listeners() {
    return listeners;
  }

  public Map<String, TaskExecutor<? extends TaskBase>> taskExecutors() {
    return taskExecutors;
  }

  public TaskExecutorFactory taskFactory() {
    return taskFactory;
  }

  public Optional<WorkflowFilter> outputFilter() {
    return outputFilter;
  }

  public Optional<SchemaValidator> outputSchemaValidator() {
    return outputSchemaValidator;
  }

  public ExpressionFactory expressionFactory() {
    return exprFactory;
  }

  public SchemaValidatorFactory validatorFactory() {
    return schemaValidatorFactory;
  }

  public ResourceLoader resourceLoader() {

    return resourceLoader;
  }

  public WorkflowPositionFactory positionFactory() {
    return positionFactory;
  }

  @Override
  public void close() {
    // TODO close resourcers hold for uncompleted process instances, if any
  }
}
