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
import io.serverlessworkflow.api.types.OneEventConsumptionStrategy;
import io.serverlessworkflow.api.types.Until;
import io.serverlessworkflow.fluent.spec.spi.EventConsumptionStrategyFluent;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractEventConsumptionStrategyBuilder<
        SELF extends EventConsumptionStrategyFluent<SELF, T>, T extends Serializable>
    implements EventConsumptionStrategyFluent<SELF, T> {

  protected boolean oneSet, allSet, anySet;
  private Until until;

  AbstractEventConsumptionStrategyBuilder() {}

  @SuppressWarnings("unchecked")
  private SELF self() {
    return (SELF) this;
  }

  public SELF one(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    oneSet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    OneEventConsumptionStrategy strat = new OneEventConsumptionStrategy();
    strat.setOne(fb.build());
    this.setOne(strat);
    return this.self();
  }

  abstract void setOne(OneEventConsumptionStrategy strategy);

  public SELF all(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    allSet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    AllEventConsumptionStrategy strat = new AllEventConsumptionStrategy();
    strat.setAll(List.of(fb.build()));
    this.setAll(strat);
    return this.self();
  }

  abstract void setAll(AllEventConsumptionStrategy strategy);

  public SELF any(Consumer<EventFilterBuilder> c) {
    ensureNoneSet();
    anySet = true;
    EventFilterBuilder fb = new EventFilterBuilder();
    c.accept(fb);
    AnyEventConsumptionStrategy strat = new AnyEventConsumptionStrategy();
    strat.setAny(List.of(fb.build()));
    this.setAny(strat);
    return this.self();
  }

  abstract void setAny(AnyEventConsumptionStrategy strategy);

  public SELF until(Consumer<EventConsumptionStrategyBuilder> c) {
    final EventConsumptionStrategyBuilder eventConsumptionStrategyBuilder =
        new EventConsumptionStrategyBuilder();
    c.accept(eventConsumptionStrategyBuilder);
    this.until = new Until().withAnyEventUntilConsumed(eventConsumptionStrategyBuilder.build());
    return this.self();
  }

  public SELF until(String expression) {
    this.until = new Until().withAnyEventUntilCondition(expression);
    return this.self();
  }

  private void ensureNoneSet() {
    if (oneSet || allSet || anySet) {
      throw new IllegalStateException("Only one consumption strategy can be configured");
    }
  }

  public final T build() {
    if (!(oneSet || allSet || anySet)) {
      throw new IllegalStateException(
          "A consumption strategy (one, all, or any) must be configured");
    }

    if (anySet) {
      this.setUntil(until);
    }
    return this.getEventConsumptionStrategy();
  }

  abstract T getEventConsumptionStrategy();

  abstract void setUntil(Until until);
}
