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
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTaskConfiguration.ListenAndReadAs;
import io.serverlessworkflow.api.types.SubscriptionIterator;
import io.serverlessworkflow.api.types.Until;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowMutableInstance;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderCollection;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderInfo;
import io.serverlessworkflow.impl.events.EventRegistrationInfo;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class ListenExecutor extends RegularTaskExecutor<ListenTask> {

  protected final EventRegistrationBuilderInfo builderRegistrationInfo;
  protected final Optional<TaskExecutor<?>> loop;
  protected final Function<CloudEvent, WorkflowModel> converter;
  protected final EventConsumer eventConsumer;

  public static class ListenExecutorBuilder extends RegularTaskExecutorBuilder<ListenTask> {

    private EventRegistrationBuilderInfo registrationInfo;
    private TaskExecutor<?> loop;
    private Function<CloudEvent, WorkflowModel> converter =
        ce -> application.modelFactory().from(ce.getData());

    protected ListenExecutorBuilder(
        WorkflowMutablePosition position, ListenTask task, WorkflowDefinition definition) {
      super(position, task, definition);
      ListenTaskConfiguration listen = task.getListen();
      registrationInfo =
          EventRegistrationBuilderInfo.from(application, listen.getTo(), this::buildUntilPredicate);
      SubscriptionIterator forEach = task.getForeach();
      if (forEach != null) {
        loop = TaskExecutorHelper.createExecutorList(position, forEach.getDo(), definition);
      }
      ListenAndReadAs readAs = listen.getRead();
      if (readAs != null) {
        switch (readAs) {
          case ENVELOPE:
            converter = ce -> application.modelFactory().from(ce);
          default:
          case DATA:
            converter = ce -> application.modelFactory().from(ce.getData());
            break;
        }
      }
    }

    protected WorkflowPredicate buildUntilPredicate(Until until) {
      return until.getAnyEventUntilCondition() != null
          ? WorkflowUtils.buildPredicate(application, until.getAnyEventUntilCondition())
          : null;
    }

    @Override
    public ListenExecutor buildInstance() {
      return registrationInfo.registrations().isAnd()
          ? new AndListenExecutor(this)
          : new OrListenExecutor(this);
    }
  }

  public static class AndListenExecutor extends ListenExecutor {

    public AndListenExecutor(ListenExecutorBuilder builder) {
      super(builder);
    }

    protected void internalProcessCe(
        WorkflowModel node,
        WorkflowModelCollection arrayNode,
        WorkflowContext workflow,
        TaskContext taskContext,
        CompletableFuture<WorkflowModel> future) {
      arrayNode.add(node);
      future.complete(node);
    }
  }

  public static class OrListenExecutor extends ListenExecutor {

    private final Optional<WorkflowPredicate> until;
    private final EventRegistrationBuilderCollection untilRegBuilders;

    public OrListenExecutor(ListenExecutorBuilder builder) {
      super(builder);
      this.until = Optional.ofNullable(builder.registrationInfo.until());
      this.untilRegBuilders = builder.registrationInfo.untilRegistrations();
    }

    @Override
    protected <T> EventRegistrationInfo buildInfo(
        BiConsumer<CloudEvent, CompletableFuture<T>> consumer) {
      EventRegistrationInfo info = super.buildInfo(consumer);
      if (untilRegBuilders != null) {
        EventRegistrationInfo untilInfo =
            EventRegistrationInfo.build(
                untilRegBuilders, (ce, f) -> f.complete(null), eventConsumer);
        untilInfo
            .completableFuture()
            .thenAccept(
                v -> {
                  info.completableFuture().complete(null);
                  untilInfo.registrations().forEach(reg -> eventConsumer.unregister(reg));
                });
      }
      return info;
    }

    protected void internalProcessCe(
        WorkflowModel node,
        WorkflowModelCollection arrayNode,
        WorkflowContext workflow,
        TaskContext taskContext,
        CompletableFuture<WorkflowModel> future) {
      arrayNode.add(node);
      if (until.map(u -> u.test(workflow, taskContext, arrayNode)).orElse(true)
          && untilRegBuilders == null) {
        future.complete(node);
      } else {
        ((WorkflowMutableInstance) workflow.instance()).status(WorkflowStatus.WAITING);
      }
    }
  }

  protected abstract void internalProcessCe(
      WorkflowModel node,
      WorkflowModelCollection arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<WorkflowModel> future);

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    WorkflowModelCollection output =
        workflow.definition().application().modelFactory().createCollection();
    ((WorkflowMutableInstance) workflow.instance()).status(WorkflowStatus.WAITING);
    EventRegistrationInfo info =
        buildInfo(
            (BiConsumer<CloudEvent, CompletableFuture<WorkflowModel>>)
                ((ce, future) ->
                    processCe(converter.apply(ce), output, workflow, taskContext, future)));
    return info.completableFuture()
        .thenApply(
            v -> {
              info.registrations().forEach(eventConsumer::unregister);
              return output;
            });
  }

  protected <T> EventRegistrationInfo buildInfo(
      BiConsumer<CloudEvent, CompletableFuture<T>> consumer) {
    return EventRegistrationInfo.build(
        builderRegistrationInfo.registrations(), consumer, eventConsumer);
  }

  private void processCe(
      WorkflowModel node,
      WorkflowModelCollection arrayNode,
      WorkflowContext workflow,
      TaskContext taskContext,
      CompletableFuture<WorkflowModel> future) {
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
    this.builderRegistrationInfo = builder.registrationInfo;
    this.loop = Optional.ofNullable(builder.loop);
    this.converter = builder.converter;
  }
}
