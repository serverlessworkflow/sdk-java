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
package io.serverlessworkflow.impl.scheduler;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class DefaultWorkflowScheduler implements WorkflowScheduler {

  private final Map<WorkflowDefinition, Collection<WorkflowInstance>> instances =
      new ConcurrentHashMap<>();

  private final ScheduledExecutorService service;
  private final CronResolverFactory cronFactory;

  public DefaultWorkflowScheduler() {
    this(Executors.newSingleThreadScheduledExecutor(), new CronUtilsResolverFactory());
  }

  public DefaultWorkflowScheduler(
      ScheduledExecutorService service, CronResolverFactory cronFactory) {
    this.service = service;
    this.cronFactory = cronFactory;
  }

  @Override
  public Collection<WorkflowInstance> scheduledInstances(WorkflowDefinition definition) {
    return Collections.unmodifiableCollection(theInstances(definition));
  }

  @Override
  public ScheduledEventConsumer eventConsumer(
      WorkflowDefinition definition,
      Function<CloudEvent, WorkflowModel> converter,
      EventRegistrationBuilderInfo builderInfo) {
    return new ScheduledEventConsumer(
        definition, converter, builderInfo, new DefaultScheduledInstanceRunner(definition));
  }

  @Override
  public Cancellable scheduleAfter(WorkflowDefinition definition, Duration delay) {
    return new ScheduledServiceCancellable(
        service.schedule(
            new DefaultScheduledInstanceRunner(definition),
            delay.toMillis(),
            TimeUnit.MILLISECONDS));
  }

  @Override
  public Cancellable scheduleEvery(WorkflowDefinition definition, Duration interval) {
    long delay = interval.toMillis();
    return new ScheduledServiceCancellable(
        service.scheduleAtFixedRate(
            new DefaultScheduledInstanceRunner(definition), delay, delay, TimeUnit.MILLISECONDS));
  }

  @Override
  public Cancellable scheduleCron(WorkflowDefinition definition, String cron) {
    return new CronResolverCancellable(definition, cronFactory.parseCron(cron));
  }

  private Collection<WorkflowInstance> theInstances(WorkflowDefinition definition) {
    return instances.computeIfAbsent(definition, def -> new ArrayList<>());
  }

  private class CronResolverCancellable implements Cancellable {
    private final WorkflowDefinition definition;
    private final CronResolver cronResolver;

    private AtomicReference<ScheduledFuture<?>> nextCron = new AtomicReference<>();
    private AtomicBoolean cancelled = new AtomicBoolean();

    public CronResolverCancellable(WorkflowDefinition definition, CronResolver cronResolver) {
      this.definition = definition;
      this.cronResolver = cronResolver;
      scheduleNext();
    }

    private void scheduleNext() {
      cronResolver
          .nextExecution()
          .ifPresent(
              d ->
                  nextCron.set(
                      service.schedule(
                          new CronResolverIntanceRunner(definition),
                          d.toMillis(),
                          TimeUnit.MILLISECONDS)));
    }

    @Override
    public void cancel() {
      cancelled.set(true);
      ScheduledFuture<?> toBeCancel = nextCron.get();
      if (toBeCancel != null) {
        toBeCancel.cancel(true);
      }
    }

    private class CronResolverIntanceRunner extends DefaultScheduledInstanceRunner {
      protected CronResolverIntanceRunner(WorkflowDefinition definition) {
        super(definition);
      }

      @Override
      public void accept(WorkflowModel model) {
        if (!cancelled.get()) {
          scheduleNext();
          super.accept(model);
        }
      }
    }
  }

  private class DefaultScheduledInstanceRunner extends ScheduledInstanceRunnable {
    protected DefaultScheduledInstanceRunner(WorkflowDefinition definition) {
      super(definition);
    }

    @Override
    protected void addScheduledInstance(WorkflowInstance instance) {
      theInstances(definition).add(instance);
    }
  }
}
