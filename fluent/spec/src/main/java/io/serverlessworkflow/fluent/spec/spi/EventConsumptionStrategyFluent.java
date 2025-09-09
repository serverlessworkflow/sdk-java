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
package io.serverlessworkflow.fluent.spec.spi;

import io.serverlessworkflow.fluent.spec.AbstractEventFilterBuilder;
import io.serverlessworkflow.fluent.spec.EventConsumptionStrategyBuilder;
import java.io.Serializable;
import java.util.function.Consumer;

public interface EventConsumptionStrategyFluent<
    SELF extends EventConsumptionStrategyFluent<SELF, T, F>,
    T extends Serializable,
    F extends AbstractEventFilterBuilder<?, ?>> {

  SELF one(Consumer<F> c);

  SELF all(Consumer<F>... c);

  SELF any(Consumer<F>... c);

  SELF any(Consumer<F> c);

  SELF until(Consumer<EventConsumptionStrategyBuilder> c);

  SELF until(String expression);

  T build();
}
