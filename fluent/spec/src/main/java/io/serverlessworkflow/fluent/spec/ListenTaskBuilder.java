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

import io.serverlessworkflow.api.types.AllEventConsumptionStrategy;
import io.serverlessworkflow.api.types.AnyEventConsumptionStrategy;
import io.serverlessworkflow.api.types.CorrelateProperty;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventFilterCorrelate;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTo;
import io.serverlessworkflow.api.types.OneEventConsumptionStrategy;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for a "listen" task in a Serverless Workflow. Enforces exactly one consumption
 * strategy: one, all, or any.
 */
public class ListenTaskBuilder<T extends BaseTaskItemListBuilder<T>>
    extends TaskBaseBuilder<ListenTaskBuilder<T>> {

  private final ListenTask listenTask;
  private final ListenTaskConfiguration config;
  private boolean oneSet, allSet, anySet;
  private final T taskItemListBuilder;

  public ListenTaskBuilder(T taskItemListBuilder) {
    super();
    this.listenTask = new ListenTask();
    this.config = new ListenTaskConfiguration();
    this.config.setTo(new ListenTo());
    this.listenTask.setListen(config);
    this.taskItemListBuilder = taskItemListBuilder;
    super.setTask(listenTask);
  }

  @Override
  protected ListenTaskBuilder<T> self() {
    return this;
  }

  /** Consume exactly one matching event. */
  public ListenTaskBuilder<T> one(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    oneSet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    OneEventConsumptionStrategy strat = new OneEventConsumptionStrategy();
    strat.setOne(fb.build());
    config.getTo().withOneEventConsumptionStrategy(strat);
    return this;
  }

  /** Consume events only when *all* filters match. */
  public ListenTaskBuilder<T> all(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    allSet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    AllEventConsumptionStrategy strat = new AllEventConsumptionStrategy();
    strat.setAll(List.of(fb.build()));
    config.getTo().withAllEventConsumptionStrategy(strat);
    return this;
  }

  /** Consume events when *any* filter matches. */
  public ListenTaskBuilder<T> any(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    anySet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    AnyEventConsumptionStrategy strat = new AnyEventConsumptionStrategy();
    strat.setAny(List.of(fb.build()));
    config.getTo().withAnyEventConsumptionStrategy(strat);
    return this;
  }

  public ListenTaskBuilder<T> forEach(Consumer<SubscriptionIteratorBuilder<T>> c) {
    final SubscriptionIteratorBuilder<T> iteratorBuilder =
        new SubscriptionIteratorBuilder<>(this.taskItemListBuilder);
    c.accept(iteratorBuilder);
    this.listenTask.setForeach(iteratorBuilder.build());
    return this;
  }

  public ListenTaskBuilder<T> read(ListenTaskConfiguration.ListenAndReadAs listenAndReadAs) {
    this.config.setRead(listenAndReadAs);
    return this;
  }

  private void ensureNoneSet() {
    if (oneSet || allSet || anySet) {
      throw new IllegalStateException("Only one consumption strategy can be configured");
    }
  }

  /** Validate and return the built ListenTask. */
  public ListenTask build() {
    if (!(oneSet || allSet || anySet)) {
      throw new IllegalStateException(
          "A consumption strategy (one, all, or any) must be configured");
    }
    return listenTask;
  }

  /** Builder for event filters used in consumption strategies. */
  public static final class EventFilterBuilder {
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
    public EventFilterBuilder correlate(String key, Consumer<CorrelatePropertyBuilder> c) {
      CorrelatePropertyBuilder cpb = new CorrelatePropertyBuilder();
      c.accept(cpb);
      correlate.withAdditionalProperty(key, cpb.build());
      return this;
    }

    public EventFilter build() {
      filter.setCorrelate(correlate);
      return filter;
    }
  }

  public static final class CorrelatePropertyBuilder {
    private final CorrelateProperty prop = new CorrelateProperty();

    public CorrelatePropertyBuilder from(String expr) {
      prop.setFrom(expr);
      return this;
    }

    public CorrelatePropertyBuilder expect(String val) {
      prop.setExpect(val);
      return this;
    }

    public CorrelateProperty build() {
      return prop;
    }
  }
}
