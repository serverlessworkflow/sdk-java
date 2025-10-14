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
import static io.serverlessworkflow.impl.WorkflowUtils.safeClose;

import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.ListenTo;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.Schedule;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderInfo;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorHelper;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import io.serverlessworkflow.impl.scheduler.ScheduledEventConsumer;
import io.serverlessworkflow.impl.schema.SchemaValidator;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorkflowDefinition implements AutoCloseable, WorkflowDefinitionData {

  private final Workflow workflow;
  private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
  private Optional<WorkflowFilter> inputFilter = Optional.empty();
  private Optional<WorkflowFilter> outputFilter = Optional.empty();
  private final WorkflowApplication application;
  private final TaskExecutor<?> taskExecutor;
  private final ResourceLoader resourceLoader;
  private final Map<String, TaskExecutor<?>> executors = new HashMap<>();
  private ScheduledEventConsumer scheculedConsumer;

  private WorkflowDefinition(
      WorkflowApplication application, Workflow workflow, ResourceLoader resourceLoader) {
    this.workflow = workflow;
    this.application = application;
    this.resourceLoader = resourceLoader;

    Input input = workflow.getInput();
    if (input != null) {
      this.inputSchemaValidator =
          getSchemaValidator(application.validatorFactory(), resourceLoader, input.getSchema());
      this.inputFilter = buildWorkflowFilter(application, input.getFrom());
    }

    Output output = workflow.getOutput();
    if (output != null) {
      this.outputSchemaValidator =
          getSchemaValidator(application.validatorFactory(), resourceLoader, output.getSchema());
      this.outputFilter = buildWorkflowFilter(application, output.getAs());
    }
    this.taskExecutor =
        TaskExecutorHelper.createExecutorList(
            application.positionFactory().get(), workflow.getDo(), this);
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow) {
    return of(application, workflow, null);
  }

  static WorkflowDefinition of(WorkflowApplication application, Workflow workflow, Path path) {
    WorkflowDefinition definition =
        new WorkflowDefinition(
            application, workflow, application.resourceLoaderFactory().getResourceLoader(path));
    Schedule schedule = workflow.getSchedule();
    if (schedule != null) {
      ListenTo to = schedule.getOn();
      if (to != null) {
        definition.scheculedConsumer =
            application
                .scheduler()
                .eventConsumer(
                    definition,
                    application.modelFactory()::from,
                    EventRegistrationBuilderInfo.from(application, to, x -> null));
      }
    }
    return definition;
  }

  public WorkflowInstance instance(Object input) {
    WorkflowModel inputModel = application.modelFactory().fromAny(input);
    inputSchemaValidator().ifPresent(v -> v.validate(inputModel));
    return new WorkflowMutableInstance(this, application().idFactory().get(), inputModel);
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

  @Override
  public Workflow workflow() {
    return workflow;
  }

  Optional<WorkflowFilter> outputFilter() {
    return outputFilter;
  }

  Optional<SchemaValidator> outputSchemaValidator() {
    return outputSchemaValidator;
  }

  @Override
  public WorkflowApplication application() {
    return application;
  }

  public ResourceLoader resourceLoader() {
    return resourceLoader;
  }

  public TaskExecutor<?> taskExecutor(String jsonPointer) {
    return executors.get(jsonPointer);
  }

  public void addTaskExecutor(WorkflowMutablePosition position, TaskExecutor<?> taskExecutor) {
    executors.put(position.jsonPointer(), taskExecutor);
  }

  @Override
  public void close() {
    safeClose(scheculedConsumer);
  }
}
