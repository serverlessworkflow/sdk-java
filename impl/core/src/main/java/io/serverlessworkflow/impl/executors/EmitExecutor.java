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
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.serverlessworkflow.api.types.EmitTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.events.EventPropertiesFilter;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class EmitExecutor extends RegularTaskExecutor<EmitTask> {

  private final EventPropertiesFilter props;

  public static class EmitExecutorBuilder extends RegularTaskExecutorBuilder<EmitTask> {

    private EventPropertiesFilter eventBuilder;

    protected EmitExecutorBuilder(
        WorkflowPosition position,
        EmitTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      this.eventBuilder =
          EventPropertiesFilter.build(
              task.getEmit().getEvent().getWith(), application.expressionFactory());
    }

    @Override
    public TaskExecutor<EmitTask> buildInstance() {
      return new EmitExecutor(this);
    }
  }

  private EmitExecutor(EmitExecutorBuilder builder) {
    super(builder);
    this.props = builder.eventBuilder;
  }

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return workflow
        .definition()
        .application()
        .eventPublisher()
        .publish(buildCloudEvent(workflow, taskContext))
        .thenApply(v -> taskContext.input());
  }

  private CloudEvent buildCloudEvent(WorkflowContext workflow, TaskContext taskContext) {
    io.cloudevents.core.v1.CloudEventBuilder ceBuilder = CloudEventBuilder.v1();
    props
        .idFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withId(value));
    props
        .timeFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withTime(value));
    props
        .sourceFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withSource(URI.create(value)));
    props
        .typeFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withType(value));
    props
        .subjectFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withSubject(value));
    props
        .dataSchemaFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withDataSchema(URI.create(value)));
    props
        .contentTypeFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withDataContentType(value));
    props
        .dataFilter()
        .map(filter -> filter.apply(workflow, taskContext, taskContext.input()))
        .ifPresent(value -> ceBuilder.withData(JsonCloudEventData.wrap(value)));
    props
        .dataFilter()
        .map(filter -> filter.apply(workflow, taskContext, taskContext.input()))
        .ifPresent(
            value ->
                value
                    .fields()
                    .forEachRemaining(
                        e -> CloudEventUtils.addExtension(ceBuilder, e.getKey(), e.getValue())));
    return ceBuilder.build();
  }
}
