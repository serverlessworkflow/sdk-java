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
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorHelper;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class WorkflowDefinition implements AutoCloseable {

  private final Workflow workflow;
  private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
  private Optional<WorkflowFilter> inputFilter = Optional.empty();
  private Optional<WorkflowFilter> outputFilter = Optional.empty();
  private final WorkflowApplication application;
  private final TaskExecutor<?> taskExecutor;

  private WorkflowDefinition(
      WorkflowApplication application, Workflow workflow, ResourceLoader resourceLoader) {
    this.workflow = workflow;
    this.application = application;
    if (workflow.getInput() != null) {
      Input input = workflow.getInput();
      this.inputSchemaValidator =
          getSchemaValidator(application.validatorFactory(), resourceLoader, input.getSchema());
      this.inputFilter = buildWorkflowFilter(application, input.getFrom());
    }
    if (workflow.getOutput() != null) {
      Output output = workflow.getOutput();
      this.outputSchemaValidator =
          getSchemaValidator(application.validatorFactory(), resourceLoader, output.getSchema());
      this.outputFilter = buildWorkflowFilter(application, output.getAs());
    }
    this.taskExecutor =
        TaskExecutorHelper.createExecutorList(
            application.positionFactory().get(),
            workflow.getDo(),
            workflow,
            application,
            resourceLoader);
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow) {
    return of(application, workflow, null);
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow, Path path) {
    return new WorkflowDefinition(
        application, workflow, application.resourceLoaderFactory().getResourceLoader(path));
  }

  public WorkflowInstance instance(Object input) {
    return new WorkflowInstance(this, application.modelFactory().fromAny(input));
  }

  Optional<SchemaValidator> inputSchemaValidator() {
    return inputSchemaValidator;
  }

  TaskExecutor<?> startTask() {
    return taskExecutor;
  }

  Optional<WorkflowFilter> inputFilter() {
    return inputFilter;
  }

  public Workflow workflow() {
    return workflow;
  }

  public Collection<WorkflowExecutionListener> listeners() {
    return application.listeners();
  }

  Optional<WorkflowFilter> outputFilter() {
    return outputFilter;
  }

  Optional<SchemaValidator> outputSchemaValidator() {
    return outputSchemaValidator;
  }

  public RuntimeDescriptorFactory runtimeDescriptorFactory() {
    return application.runtimeDescriptorFactory();
  }

  public WorkflowApplication application() {
    return application;
  }

  @Override
  public void close() {
    // TODO close resourcers hold for uncompleted process instances, if any
  }
}
