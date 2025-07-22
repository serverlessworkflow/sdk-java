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

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.api.types.EmitTask;
import io.serverlessworkflow.api.types.EventData;
import io.serverlessworkflow.api.types.EventDataschema;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.EventSource;
import io.serverlessworkflow.api.types.EventTime;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.ExpressionHolder;
import io.serverlessworkflow.impl.StringFilter;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EmitExecutor extends RegularTaskExecutor<EmitTask> {

  private final EventPropertiesBuilder props;

  public static class EmitExecutorBuilder extends RegularTaskExecutorBuilder<EmitTask> {

    private EventPropertiesBuilder eventBuilder;

    protected EmitExecutorBuilder(
        WorkflowPosition position,
        EmitTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      this.eventBuilder =
          EventPropertiesBuilder.build(task.getEmit().getEvent().getWith(), application);
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
  protected CompletableFuture<WorkflowModel> internalExecute(
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
    ceBuilder.withId(
        props
            .idFilter()
            .map(filter -> filter.apply(workflow, taskContext))
            .orElse(UUID.randomUUID().toString()));
    ceBuilder.withSource(
        props
            .sourceFilter()
            .map(filter -> filter.apply(workflow, taskContext))
            .map(URI::create)
            .orElse(URI.create("reference-impl")));
    ceBuilder.withType(
        props
            .typeFilter()
            .map(filter -> filter.apply(workflow, taskContext))
            .orElseThrow(
                () -> new IllegalArgumentException("Type is required for emitting events")));
    props
        .timeFilter()
        .map(filter -> filter.apply(workflow, taskContext))
        .ifPresent(value -> ceBuilder.withTime(value));
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
        .ifPresent(
            value ->
                ceBuilder.withData(
                    value
                        .asCloudEventData()
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException(
                                    "Workflow model "
                                        + value
                                        + " cannot be converted to CloudEvent"))));
    // TODO JsonCloudEventData.wrap(value)
    props
        .additionalFilter()
        .map(filter -> filter.apply(workflow, taskContext, taskContext.input()))
        .ifPresent(value -> value.forEach((k, v) -> addExtension(ceBuilder, k, v)));

    return ceBuilder.build();
  }

  private static CloudEventBuilder addExtension(
      CloudEventBuilder builder, String name, WorkflowModel value) {
    value
        .asText()
        .ifPresentOrElse(
            v -> builder.withExtension(name, v),
            () ->
                value
                    .asBoolean()
                    .ifPresentOrElse(
                        v -> builder.withExtension(name, v),
                        () -> value.asNumber().ifPresent(v -> builder.withExtension(name, v))));
    return builder;
  }

  private static record EventPropertiesBuilder(
      Optional<StringFilter> idFilter,
      Optional<StringFilter> sourceFilter,
      Optional<StringFilter> subjectFilter,
      Optional<StringFilter> contentTypeFilter,
      Optional<StringFilter> typeFilter,
      Optional<StringFilter> dataSchemaFilter,
      Optional<ExpressionHolder<OffsetDateTime>> timeFilter,
      Optional<WorkflowFilter> dataFilter,
      Optional<WorkflowFilter> additionalFilter) {

    public static EventPropertiesBuilder build(
        EventProperties properties, WorkflowApplication app) {
      Optional<StringFilter> idFilter = buildFilter(app, properties.getId());
      EventSource source = properties.getSource();
      Optional<StringFilter> sourceFilter =
          source == null
              ? Optional.empty()
              : Optional.of(
                  WorkflowUtils.buildStringFilter(
                      app,
                      source.getRuntimeExpression(),
                      WorkflowUtils.toString(source.getUriTemplate())));
      Optional<StringFilter> subjectFilter = buildFilter(app, properties.getSubject());
      Optional<StringFilter> contentTypeFilter = buildFilter(app, properties.getDatacontenttype());
      Optional<StringFilter> typeFilter = buildFilter(app, properties.getType());
      EventDataschema dataSchema = properties.getDataschema();
      Optional<StringFilter> dataSchemaFilter =
          dataSchema == null
              ? Optional.empty()
              : Optional.of(
                  WorkflowUtils.buildStringFilter(
                      app,
                      dataSchema.getExpressionDataSchema(),
                      WorkflowUtils.toString(dataSchema.getLiteralDataSchema())));
      EventTime time = properties.getTime();
      Optional<ExpressionHolder<OffsetDateTime>> timeFilter =
          time == null
              ? Optional.empty()
              : Optional.of(
                  WorkflowUtils.buildExpressionHolder(
                      app,
                      time.getRuntimeExpression(),
                      CloudEventUtils.toOffset(time.getLiteralTime()),
                      v ->
                          v.asDate()
                              .orElseThrow(
                                  () ->
                                      new IllegalArgumentException(
                                          "Expression does not generate a valid date"))));
      EventData data = properties.getData();
      Optional<WorkflowFilter> dataFilter =
          properties.getData() == null
              ? Optional.empty()
              : Optional.of(
                  WorkflowUtils.buildWorkflowFilter(
                      app, data.getRuntimeExpression(), data.getObject()));
      Map<String, Object> ceAttrs = properties.getAdditionalProperties();
      Optional<WorkflowFilter> additionalFilter =
          ceAttrs == null || ceAttrs.isEmpty()
              ? Optional.empty()
              : Optional.of(WorkflowUtils.buildWorkflowFilter(app, null, ceAttrs));
      return new EventPropertiesBuilder(
          idFilter,
          sourceFilter,
          subjectFilter,
          contentTypeFilter,
          typeFilter,
          dataSchemaFilter,
          timeFilter,
          dataFilter,
          additionalFilter);
    }

    private static Optional<StringFilter> buildFilter(WorkflowApplication appl, String str) {
      return str == null
          ? Optional.empty()
          : Optional.of(WorkflowUtils.buildStringFilter(appl, str));
    }
  }
}
