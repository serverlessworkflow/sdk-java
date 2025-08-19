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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventFilterCorrelate;
import java.util.function.Consumer;

/** Builder for event filters used in consumption strategies. */
public final class EventFilterBuilder {
  private final EventFilter filter = new EventFilter();
  private final EventFilterCorrelate correlate = new EventFilterCorrelate();

  /** Predicate to match event properties. */
  public EventFilterBuilder with(Consumer<EventPropertiesBuilder> c) {
    EventPropertiesBuilder pb = new EventPropertiesBuilder();
    c.accept(pb);
    filter.setWith(pb.build());
    return this;
  }

  /** Correlation property for the filter. */
  public EventFilterBuilder correlate(
      String key, Consumer<ListenTaskBuilder.CorrelatePropertyBuilder> c) {
    ListenTaskBuilder.CorrelatePropertyBuilder cpb =
        new ListenTaskBuilder.CorrelatePropertyBuilder();
    c.accept(cpb);
    correlate.withAdditionalProperty(key, cpb.build());
    return this;
  }

  public EventFilter build() {
    filter.setCorrelate(correlate);
    return filter;
  }
}
