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
import io.serverlessworkflow.impl.events.EventRegistrationBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InMemoryAllStrategyCorrelationInfo implements AllStrategyCorrelationInfo {

  private Map<EventRegistrationBuilder, List<CloudEvent>> correlatedEvents;

  @Override
  public void correlate(
      EventRegistrationBuilder reg, CloudEvent event, Consumer<Collection<CloudEvent>> starter) {
    Collection<CloudEvent> collection = new ArrayList<>();
    // to minimize the critical section, conversion is done later, here we are
    // performing just collection, if any
    synchronized (correlatedEvents) {
      correlatedEvents.get(reg).add(event);
      Collection<List<CloudEvent>> events = correlatedEvents.values();
      if (satisfyCondition(events)) {
        for (List<CloudEvent> values : events) {
          collection.add(values.remove(0));
        }
      }
    }
    if (!collection.isEmpty()) {
      starter.accept(collection);
    }
  }

  @Override
  public void register(EventRegistrationBuilder reg) {
    if (correlatedEvents == null) {
      correlatedEvents = new HashMap<>();
    }
    correlatedEvents.put(reg, new ArrayList<CloudEvent>());
  }

  private boolean satisfyCondition(Collection<List<CloudEvent>> events) {
    for (List<CloudEvent> values : events) {
      if (values.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    if (correlatedEvents != null) {
      correlatedEvents.clear();
    }
  }
}
