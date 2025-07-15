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
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.AbstractCollection;
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

  @Override
  public TypeEventRegistrationBuilder listen(
      EventFilter register, WorkflowApplication application) {
    EventProperties properties = register.getWith();
    String type = properties.getType();
    return new TypeEventRegistrationBuilder(
        type, new DefaultCloudEventPredicate(properties, application));
  }

  @Override
  public Collection<TypeEventRegistrationBuilder> listenToAll(WorkflowApplication application) {
    return List.of(new TypeEventRegistrationBuilder(null, null));
  }

  private static class CloudEventConsumer extends AbstractCollection<TypeEventRegistration>
      implements Consumer<CloudEvent> {
    private Collection<TypeEventRegistration> registrations = new CopyOnWriteArrayList<>();

    @Override
    public void accept(CloudEvent ce) {
      logger.debug("Received cloud event {}", ce);
      for (TypeEventRegistration registration : registrations) {
        if (registration.predicate().test(ce)) {
          registration.consumer().accept(ce);
        }
      }
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

  public TypeEventRegistration register(
      TypeEventRegistrationBuilder builder, Consumer<CloudEvent> ce) {
    if (builder.type() == null) {
      registerToAll(ce);
      return new TypeEventRegistration(null, ce, null);
    } else {
      TypeEventRegistration registration =
          new TypeEventRegistration(builder.type(), ce, builder.cePredicate());
      registrations
          .computeIfAbsent(
              registration.type(),
              k -> {
                CloudEventConsumer consumer = new CloudEventConsumer();
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
