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
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTaskConfiguration.ListenAndReadAs;
import io.serverlessworkflow.api.types.ListenTo;
import io.serverlessworkflow.api.types.SubscriptionIterator;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.events.EventRegistration;
import io.serverlessworkflow.impl.events.EventRegistrationBuilder;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ListenExecutor extends RegularTaskExecutor<ListenTask> {

  protected final Collection<EventRegistrationBuilder> regBuilders;
  protected final Optional<WorkflowFilter> until;
  protected final Optional<TaskExecutor<?>> loop;
  protected final Function<CloudEvent, JsonNode> converter;

  public static class ListenExecutorBuilder extends RegularTaskExecutorBuilder<ListenTask> {

    private Collection<EventRegistrationBuilder> registrations;
    private WorkflowFilter until;
    private TaskExecutor<?> loop;
    private Function<CloudEvent, JsonNode> converter = this::defaultCEConverter;
    private boolean isAnd;

    protected ListenExecutorBuilder(
        WorkflowPosition position,
        ListenTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      ListenTaskConfiguration listen = task.getListen();
      ListenTo to = listen.getTo();
      if (to.getAllEventConsumptionStrategy() != null) {
        isAnd = true;
        registrations = from(to.getAllEventConsumptionStrategy().getAll());
      } else if (to.getAnyEventConsumptionStrategy() != null) {
        isAnd = false;
        registrations = from(to.getAnyEventConsumptionStrategy().getAny());
      } else if (to.getOneEventConsumptionStrategy() != null) {
        isAnd = false;
        registrations = List.of(from(to.getOneEventConsumptionStrategy().getOne()));
      }
      SubscriptionIterator forEach = task.getForeach();
      if (forEach != null) {
        loop =
            TaskExecutorHelper.createExecutorList(
                position, forEach.getDo(), workflow, application, resourceLoader);
      }
      ListenAndReadAs readAs = listen.getRead();
      if (readAs != null) {
        switch (readAs) {
          case ENVELOPE:
            converter = CloudEventUtils::toJsonNode;
          default:
          case DATA:
            converter = this::defaultCEConverter;
            break;
        }
      }
    }

    private JsonNode defaultCEConverter(CloudEvent ce) {
      return CloudEventUtils.toJsonNode(ce.getData());
    }

    private Collection<EventRegistrationBuilder> from(List<EventFilter> filters) {
      return filters.stream().map(this::from).collect(Collectors.toList());
    }

    private EventRegistrationBuilder from(EventFilter filter) {
      return application.eventConsumer().build(filter, application);
    }

    @Override
    public TaskExecutor<ListenTask> buildInstance() {
      return isAnd ? new AndListenExecutor(this) : new OrListenExecutor(this);
    }
  }

  public static class AndListenExecutor extends ListenExecutor {

    public AndListenExecutor(ListenExecutorBuilder builder) {
      super(builder);
    }

    protected void internalProcessCe(
        JsonNode node,
        ArrayNode arrayNode,
        WorkflowContext workflow,
        TaskContext taskContext,
        CompletableFuture<JsonNode> future) {
      future.complete(node);
    }

    @Override
    protected CompletableFuture<?> combine(CompletableFuture<JsonNode>[] completables) {
      return CompletableFuture.allOf(completables);
    }
  }

  public static class OrListenExecutor extends ListenExecutor {

    public OrListenExecutor(ListenExecutorBuilder builder) {
      super(builder);
    }

    @Override
    protected CompletableFuture<?> combine(CompletableFuture<JsonNode>[] completables) {
      return CompletableFuture.anyOf(completables);
    }

    protected void internalProcessCe(
        JsonNode node,
        ArrayNode arrayNode,
        WorkflowContext workflow,
        TaskContext taskContext,
        CompletableFuture<JsonNode> future) {
      if (until.isEmpty()
          || until.filter(u -> u.apply(workflow, taskContext, arrayNode).asBoolean()).isPresent()) {
        future.complete(arrayNode);
      }
    }
  }

  protected abstract CompletableFuture<?> combine(CompletableFuture<JsonNode>[] completables);

  protected abstract void internalProcessCe(
      JsonNode node,
      ArrayNode arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<JsonNode> future);

  private void processCe(
      JsonNode node,
      ArrayNode arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<JsonNode> future) {
    arrayNode.add(arrayNode);
    loop.ifPresentOrElse(
        t -> {
          SubscriptionIterator forEach = task.getForeach();
          String item = forEach.getItem();
          if (item != null) {
            taskContext.variables().put(item, node);
          }
          String at = forEach.getAt();
          if (item != null) {
            taskContext.variables().put(at, arrayNode.size());
          }
          TaskExecutorHelper.processTaskList(t, workflow, Optional.of(taskContext), node)
              .thenAccept(n -> internalProcessCe(n, arrayNode, workflow, taskContext, future));
        },
        () -> internalProcessCe(node, arrayNode, workflow, taskContext, future));
  }

  protected CompletableFuture<JsonNode> toCompletable(
      WorkflowContext workflow,
      TaskContext taskContext,
      EventRegistrationBuilder regBuilder,
      Collection<EventRegistration> registrations,
      ArrayNode arrayNode) {
    final CompletableFuture<JsonNode> future = new CompletableFuture<>();
    registrations.add(
        workflow
            .definition()
            .application()
            .eventConsumer()
            .register(
                regBuilder,
                (Consumer<CloudEvent>)
                    (ce ->
                        processCe(converter.apply(ce), arrayNode, workflow, taskContext, future)),
                workflow,
                taskContext));
    return future;
  }

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    ArrayNode output = JsonUtils.mapper().createArrayNode();
    Collection<EventRegistration> registrations = new ArrayList<>();
    return combine(
            regBuilders.stream()
                .map(reg -> toCompletable(workflow, taskContext, reg, registrations, output))
                .toArray(size -> new CompletableFuture[size]))
        .thenApply(
            v -> {
              registrations.forEach(
                  reg -> workflow.definition().application().eventConsumer().unregister(reg));
              return output;
            });
  }

  protected ListenExecutor(ListenExecutorBuilder builder) {
    super(builder);
    this.regBuilders = builder.registrations;
    this.until = Optional.ofNullable(builder.until);
    this.loop = Optional.ofNullable(builder.loop);
    this.converter = builder.converter;
  }
}
