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

  private static class InMemoryAllStrategyCorrelationInfoHolder {
    private static InMemoryAllStrategyCorrelationInfo INSTANCE =
        new InMemoryAllStrategyCorrelationInfo();
  }

  public static AllStrategyCorrelationInfo instance() {
    return InMemoryAllStrategyCorrelationInfoHolder.INSTANCE;
  }

  private InMemoryAllStrategyCorrelationInfo() {}

  private Map<EventRegistrationBuilder, List<CloudEvent>> correlatedEvents;
  private Consumer<Map<EventRegistrationBuilder, CloudEvent>> starter;

  @Override
  public void correlate(EventRegistrationBuilder reg, CloudEvent event) {
    Map<EventRegistrationBuilder, CloudEvent> result = new HashMap<>();
    // to minimize the critical section, conversion is done later, here we are
    // performing just collection, if any
    synchronized (correlatedEvents) {
      correlatedEvents.get(reg).add(event);
      if (satisfyCondition(correlatedEvents)) {
        for (java.util.Map.Entry<EventRegistrationBuilder, List<CloudEvent>> values :
            correlatedEvents.entrySet()) {
          result.put(values.getKey(), values.getValue().remove(0));
        }
      }
    }
    if (!result.isEmpty()) {
      starter.accept(result);
    }
  }

  @Override
  public void init(
      Collection<EventRegistrationBuilder> regs,
      Consumer<Map<EventRegistrationBuilder, CloudEvent>> starter) {
    correlatedEvents = new HashMap<>();
    this.starter = starter;
    regs.forEach(reg -> correlatedEvents.put(reg, new ArrayList<CloudEvent>()));
  }

  private boolean satisfyCondition(Map<EventRegistrationBuilder, List<CloudEvent>> events) {
    for (List<CloudEvent> values : events.values()) {
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
