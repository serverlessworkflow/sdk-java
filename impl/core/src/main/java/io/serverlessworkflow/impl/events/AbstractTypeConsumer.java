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
package io.serverlessworkflow.impl.events;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.types.CorrelateProperty;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventFilterCorrelate;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTypeConsumer
    implements EventConsumer<TypeEventRegistration, TypeEventRegistrationBuilder> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractTypeConsumer.class);

  protected abstract void registerToAll(Consumer<CloudEvent> consumer);

  protected abstract void unregisterFromAll();

  protected abstract void register(String topicName, Consumer<CloudEvent> consumer);

  protected abstract void unregister(String topicName);

  private Map<String, CloudEventConsumer> registrations = new ConcurrentHashMap<>();
  private WorkflowModelFactory modelFactory;

  @Override
  public TypeEventRegistrationBuilder listen(
      EventFilter register, WorkflowApplication application) {
    this.modelFactory = application.modelFactory();
    EventProperties properties = register.getWith();
    String type = properties.getType();
    CloudEventPredicate cePredicate =
        application.cloudEventPredicateFactory().build(application, properties);
    Collection<CorrelationPredicate> correlationPredicates =
        buildCorrelationPredicates(register.getCorrelate(), application);
    return correlationPredicates.isEmpty()
        ? new TypeEventRegistrationBuilder(type, cePredicate)
        : new TypeEventRegistrationBuilder(type, cePredicate, correlationPredicates);
  }

  private Collection<CorrelationPredicate> buildCorrelationPredicates(
      EventFilterCorrelate correlate, WorkflowApplication application) {
    if (correlate == null || correlate.getAdditionalProperties().isEmpty()) {
      return List.of();
    }
    Collection<CorrelationPredicate> predicates = new ArrayList<>();
    for (Map.Entry<String, CorrelateProperty> entry :
        correlate.getAdditionalProperties().entrySet()) {
      predicates.add(CorrelationPredicate.from(entry.getKey(), entry.getValue(), application));
    }
    return predicates;
  }

  @Override
  public Collection<TypeEventRegistrationBuilder> listenToAll(WorkflowApplication application) {
    return List.of(new TypeEventRegistrationBuilder(null, null));
  }

  private static class CloudEventConsumer extends AbstractCollection<TypeEventRegistration>
      implements Consumer<CloudEvent> {
    private final WorkflowModelFactory modelFactory;
    private Collection<TypeEventRegistration> registrations = new CopyOnWriteArrayList<>();

    CloudEventConsumer(WorkflowModelFactory modelFactory) {
      this.modelFactory = modelFactory;
    }

    @Override
    public void accept(CloudEvent ce) {
      WorkflowModel eventModel = null;
      for (TypeEventRegistration registration : registrations) {
        if (registration.predicate().test(ce, registration.workflow(), registration.task())) {
          Collection<CorrelationPredicate> predicates = registration.correlationPredicates();
          if (!predicates.isEmpty()) {
            if (eventModel == null) {
              eventModel = modelFactory.from(ce);
            }
            if (!testCorrelation(eventModel, registration)) {
              continue;
            }
          }
          registration.consumer().accept(ce);
        }
      }
    }

    private boolean testCorrelation(WorkflowModel eventModel, TypeEventRegistration registration) {
      Collection<CorrelationPredicate> predicates = registration.correlationPredicates();
      if (predicates.isEmpty()) {
        return true;
      }
      for (CorrelationPredicate pred : predicates) {
        if (!pred.test(eventModel, registration.workflow(), registration.task())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean add(TypeEventRegistration registration) {
      return registrations.add(registration);
    }

    @Override
    public boolean remove(Object registration) {
      return registrations.remove(registration);
    }

    @Override
    public Iterator<TypeEventRegistration> iterator() {
      return registrations.iterator();
    }

    @Override
    public int size() {
      return registrations.size();
    }
  }

  @Override
  public TypeEventRegistration register(
      TypeEventRegistrationBuilder builder,
      Consumer<CloudEvent> ce,
      WorkflowContext workflow,
      TaskContext task) {
    if (builder.type() == null) {
      registerToAll(ce);
      return new TypeEventRegistration(null, ce, null, workflow, task);
    } else {
      TypeEventRegistration registration =
          new TypeEventRegistration(
              builder.type(),
              ce,
              builder.cePredicate(),
              builder.correlationPredicates(),
              workflow,
              task);
      registrations
          .computeIfAbsent(
              registration.type(),
              k -> {
                CloudEventConsumer consumer = new CloudEventConsumer(modelFactory);
                register(k, consumer);
                return consumer;
              })
          .add(registration);
      return registration;
    }
  }

  @Override
  public void unregister(TypeEventRegistration registration) {
    if (registration.type() == null) {
      unregisterFromAll();
    } else {
      registrations.computeIfPresent(
          registration.type(),
          (k, v) -> {
            v.remove(registration);
            if (v.isEmpty()) {
              unregister(registration.type());
              return null;
            } else {
              return v;
            }
          });
    }
  }
}
