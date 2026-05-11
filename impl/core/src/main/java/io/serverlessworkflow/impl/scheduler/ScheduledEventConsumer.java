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

import static io.serverlessworkflow.impl.WorkflowUtils.safeClose;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventRegistration;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class ScheduledEventConsumer implements AutoCloseable {

  private final Function<CloudEvent, WorkflowModel> converter;
  private final WorkflowDefinition definition;
  private final EventRegistrationBuilderInfo builderInfo;
  private final EventConsumer eventConsumer;
  private final ScheduledInstanceRunnable instanceRunner;
  private final Collection<EventRegistration> registrations = new ArrayList<>();
  private AllStrategyCorrelationInfo allStrategyCorrelationInfo;

  public ScheduledEventConsumer(
      WorkflowDefinition definition,
      Function<CloudEvent, WorkflowModel> converter,
      EventRegistrationBuilderInfo builderInfo,
      ScheduledInstanceRunnable instanceRunner) {
    this.definition = definition;
    this.converter = converter;
    this.builderInfo = builderInfo;
    this.instanceRunner = instanceRunner;
    this.eventConsumer = definition.application().eventConsumer();

    if (builderInfo.registrations().isAnd()
        && builderInfo.registrations().registrations().size() > 1) {
      this.allStrategyCorrelationInfo =
          definition.application().allStrategyCorrelationInfoFactory().apply(definition);
      builderInfo
          .registrations()
          .registrations()
          .forEach(
              reg -> {
                allStrategyCorrelationInfo.register(reg);
                registrations.add(
                    eventConsumer.register(
                        reg,
                        ce ->
                            allStrategyCorrelationInfo.correlate(
                                reg, (CloudEvent) ce, this::start)));
              });
    } else {
      builderInfo
          .registrations()
          .registrations()
          .forEach(
              reg -> registrations.add(eventConsumer.register(reg, ce -> start((CloudEvent) ce))));
    }
  }

  protected void start(CloudEvent ce) {
    WorkflowModelCollection model = definition.application().modelFactory().createCollection();
    model.add(converter.apply(ce));
    instanceRunner.accept(model);
  }

  protected void start(Collection<CloudEvent> ces) {
    WorkflowModelCollection model = definition.application().modelFactory().createCollection();
    ces.forEach(ce -> model.add(converter.apply(ce)));
    instanceRunner.accept(model);
  }

  public void close() {
    registrations.forEach(eventConsumer::unregister);
    safeClose(allStrategyCorrelationInfo);
  }
}
