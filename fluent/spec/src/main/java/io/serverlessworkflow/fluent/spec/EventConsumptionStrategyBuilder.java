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
import io.serverlessworkflow.api.types.EventConsumptionStrategy;
import io.serverlessworkflow.api.types.OneEventConsumptionStrategy;
import io.serverlessworkflow.api.types.Until;

public class EventConsumptionStrategyBuilder
    extends AbstractEventConsumptionStrategyBuilder<
        EventConsumptionStrategyBuilder, EventConsumptionStrategy> {

  private final EventConsumptionStrategy eventConsumptionStrategy = new EventConsumptionStrategy();

  EventConsumptionStrategyBuilder() {}

  @Override
  void setOne(OneEventConsumptionStrategy strategy) {
    eventConsumptionStrategy.setOneEventConsumptionStrategy(strategy);
  }

  @Override
  void setAll(AllEventConsumptionStrategy strategy) {
    eventConsumptionStrategy.setAllEventConsumptionStrategy(strategy);
  }

  @Override
  void setAny(AnyEventConsumptionStrategy strategy) {
    eventConsumptionStrategy.setAnyEventConsumptionStrategy(strategy);
  }

  @Override
  EventConsumptionStrategy getEventConsumptionStrategy() {
    return this.eventConsumptionStrategy;
  }

  @Override
  void setUntil(Until until) {
    this.eventConsumptionStrategy.getAnyEventConsumptionStrategy().setUntil(until);
  }
}
