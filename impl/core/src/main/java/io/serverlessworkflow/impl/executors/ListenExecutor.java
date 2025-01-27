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
import io.serverlessworkflow.api.types.AllEventConsumptionStrategy;
import io.serverlessworkflow.api.types.AnyEventConsumptionStrategy;
import io.serverlessworkflow.api.types.EventConsumptionStrategy;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTaskConfiguration.ListenAndReadAs;
import io.serverlessworkflow.api.types.ListenTo;
import io.serverlessworkflow.api.types.OneEventConsumptionStrategy;
import io.serverlessworkflow.api.types.SubscriptionIterator;
import io.serverlessworkflow.api.types.Until;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.events.CloudEventUtils;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventRegistration;
import io.serverlessworkflow.impl.events.EventRegistrationBuilder;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ListenExecutor extends RegularTaskExecutor<ListenTask> {

  protected final EventRegistrationBuilderCollection regBuilders;
  protected final Optional<TaskExecutor<?>> loop;
  protected final Function<CloudEvent, JsonNode> converter;
  protected final EventConsumer eventConsumer;

  private static record EventRegistrationBuilderCollection(
      Collection<EventRegistrationBuilder> registrations, boolean isAnd) {}

  public static class ListenExecutorBuilder extends RegularTaskExecutorBuilder<ListenTask> {

    private EventRegistrationBuilderCollection registrations;
    private WorkflowFilter until;
    private EventRegistrationBuilderCollection untilRegistrations;
    private TaskExecutor<?> loop;
    private Function<CloudEvent, JsonNode> converter = this::defaultCEConverter;

    private EventRegistrationBuilderCollection allEvents(AllEventConsumptionStrategy allStrategy) {
      return new EventRegistrationBuilderCollection(from(allStrategy.getAll()), true);
    }

    private EventRegistrationBuilderCollection anyEvents(AnyEventConsumptionStrategy anyStrategy) {
      List<EventFilter> eventFilters = anyStrategy.getAny();
      return new EventRegistrationBuilderCollection(
          eventFilters.isEmpty() ? registerToAll() : from(eventFilters), false);
    }

    private EventRegistrationBuilderCollection oneEvent(OneEventConsumptionStrategy oneStrategy) {
      return new EventRegistrationBuilderCollection(List.of(from(oneStrategy.getOne())), true);
    }

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
        registrations = allEvents(to.getAllEventConsumptionStrategy());
      } else if (to.getAnyEventConsumptionStrategy() != null) {
        AnyEventConsumptionStrategy any = to.getAnyEventConsumptionStrategy();
        registrations = anyEvents(any);
        Until untilDesc = any.getUntil();
        if (untilDesc != null) {
          if (untilDesc.getAnyEventUntilCondition() != null) {
            until =
                WorkflowUtils.buildWorkflowFilter(
                    application.expressionFactory(), untilDesc.getAnyEventUntilCondition());
          } else if (untilDesc.getAnyEventUntilConsumed() != null) {
            EventConsumptionStrategy strategy = untilDesc.getAnyEventUntilConsumed();
            if (strategy.getAllEventConsumptionStrategy() != null) {
              untilRegistrations = allEvents(strategy.getAllEventConsumptionStrategy());
            } else if (strategy.getAnyEventConsumptionStrategy() != null) {
              untilRegistrations = anyEvents(strategy.getAnyEventConsumptionStrategy());
            } else if (strategy.getOneEventConsumptionStrategy() != null) {
              untilRegistrations = oneEvent(strategy.getOneEventConsumptionStrategy());
            }
          }
        }
      } else if (to.getOneEventConsumptionStrategy() != null) {
        registrations = oneEvent(to.getOneEventConsumptionStrategy());
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

    private Collection<EventRegistrationBuilder> registerToAll() {
      return application.eventConsumer().listenToAll(application);
    }

    private JsonNode defaultCEConverter(CloudEvent ce) {
      return CloudEventUtils.toJsonNode(ce.getData());
    }

    private Collection<EventRegistrationBuilder> from(List<EventFilter> filters) {
      return filters.stream().map(this::from).collect(Collectors.toList());
    }

    private EventRegistrationBuilder from(EventFilter filter) {
      return application.eventConsumer().listen(filter, application);
    }

    @Override
    public TaskExecutor<ListenTask> buildInstance() {
      return registrations.isAnd() ? new AndListenExecutor(this) : new OrListenExecutor(this);
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
      arrayNode.add(node);
      future.complete(node);
    }
  }

  public static class OrListenExecutor extends ListenExecutor {

    private final Optional<WorkflowFilter> until;
    private final EventRegistrationBuilderCollection untilRegBuilders;

    public OrListenExecutor(ListenExecutorBuilder builder) {
      super(builder);
      this.until = Optional.ofNullable(builder.until);
      this.untilRegBuilders = builder.untilRegistrations;
    }

    @Override
    protected <T> CompletableFuture<?> buildFuture(
        EventRegistrationBuilderCollection regCollection,
        Collection<EventRegistration> registrations,
        BiConsumer<CloudEvent, CompletableFuture<T>> consumer) {
      CompletableFuture<?> combinedFuture =
          super.buildFuture(regCollection, registrations, consumer);
      if (untilRegBuilders != null) {
        Collection<EventRegistration> untilRegistrations = new ArrayList<>();
        CompletableFuture<?> untilFuture =
            combine(untilRegBuilders, untilRegistrations, (ce, f) -> f.complete(null));
        untilFuture.thenAccept(
            v -> {
              combinedFuture.complete(null);
              untilRegistrations.forEach(reg -> eventConsumer.unregister(reg));
            });
      }
      return combinedFuture;
    }

    protected void internalProcessCe(
        JsonNode node,
        ArrayNode arrayNode,
        WorkflowContext workflow,
        TaskContext taskContext,
        CompletableFuture<JsonNode> future) {
      arrayNode.add(node);
      if ((until.isEmpty()
              || until
                  .filter(u -> u.apply(workflow, taskContext, arrayNode).asBoolean())
                  .isPresent())
          && untilRegBuilders == null) {
        future.complete(arrayNode);
      }
    }
  }

  protected abstract void internalProcessCe(
      JsonNode node,
      ArrayNode arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<JsonNode> future);

  @Override
  protected CompletableFuture<JsonNode> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    ArrayNode output = JsonUtils.mapper().createArrayNode();
    Collection<EventRegistration> registrations = new ArrayList<>();
    return buildFuture(
            regBuilders,
            registrations,
            (BiConsumer<CloudEvent, CompletableFuture<JsonNode>>)
                ((ce, future) ->
                    processCe(converter.apply(ce), output, workflow, taskContext, future)))
        .thenApply(
            v -> {
              registrations.forEach(reg -> eventConsumer.unregister(reg));
              return output;
            });
  }

  protected <T> CompletableFuture<?> buildFuture(
      EventRegistrationBuilderCollection regCollection,
      Collection<EventRegistration> registrations,
      BiConsumer<CloudEvent, CompletableFuture<T>> consumer) {
    return combine(regCollection, registrations, consumer);
  }

  protected final <T> CompletableFuture<?> combine(
      EventRegistrationBuilderCollection regCollection,
      Collection<EventRegistration> registrations,
      BiConsumer<CloudEvent, CompletableFuture<T>> consumer) {
    CompletableFuture<T>[] futures =
        regCollection.registrations().stream()
            .map(reg -> toCompletable(reg, registrations, consumer))
            .toArray(size -> new CompletableFuture[size]);
    return regCollection.isAnd()
        ? CompletableFuture.allOf(futures)
        : CompletableFuture.anyOf(futures);
  }

  private <T> CompletableFuture<T> toCompletable(
      EventRegistrationBuilder regBuilder,
      Collection<EventRegistration> registrations,
      BiConsumer<CloudEvent, CompletableFuture<T>> ceConsumer) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    registrations.add(
        eventConsumer.register(regBuilder, ce -> ceConsumer.accept((CloudEvent) ce, future)));
    return future;
  }

  private void processCe(
      JsonNode node,
      ArrayNode arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<JsonNode> future) {
    loop.ifPresentOrElse(
        t -> {
          SubscriptionIterator forEach = task.getForeach();
          String item = forEach.getItem();
          if (item != null) {
            taskContext.variables().put(item, node);
          }
          String at = forEach.getAt();
          if (at != null) {
            taskContext.variables().put(at, arrayNode.size());
          }
          TaskExecutorHelper.processTaskList(t, workflow, Optional.of(taskContext), node)
              .thenAccept(n -> internalProcessCe(n, arrayNode, workflow, taskContext, future));
        },
        () -> internalProcessCe(node, arrayNode, workflow, taskContext, future));
  }

  protected ListenExecutor(ListenExecutorBuilder builder) {
    super(builder);
    this.eventConsumer = builder.application.eventConsumer();
    this.regBuilders = builder.registrations;
    this.loop = Optional.ofNullable(builder.loop);
    this.converter = builder.converter;
  }
}
