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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.fluent.spec.EventFilterBuilder;
import io.serverlessworkflow.fluent.spec.ListenTaskBuilder;
import io.serverlessworkflow.fluent.spec.ListenToBuilder;
import io.serverlessworkflow.fluent.spec.configurers.EventConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.ListenConfigurer;
import java.util.Objects;
import java.util.function.Consumer;

public final class ListenSpec implements ListenConfigurer {

  private Consumer<ListenToBuilder> strategyStep;
  private Consumer<ListenToBuilder> untilStep;

  @SuppressWarnings("unchecked")
  private static Consumer<EventFilterBuilder>[] asFilters(EventConfigurer[] events) {
    Consumer<EventFilterBuilder>[] filters = new Consumer[events.length];
    for (int i = 0; i < events.length; i++) {
      EventConfigurer ev = Objects.requireNonNull(events[i], "events[" + i + "]");
      filters[i] = f -> f.with(ev);
    }
    return filters;
  }

  public ListenSpec all(EventConfigurer... events) {
    strategyStep = t -> t.all(asFilters(events));
    return this;
  }

  public ListenSpec one(EventConfigurer e) {
    strategyStep = t -> t.one(f -> f.with(e));
    return this;
  }

  public ListenSpec any(EventConfigurer... events) {
    strategyStep = t -> t.any(asFilters(events));
    return this;
  }

  public ListenSpec until(String expression) {
    untilStep = t -> t.until(expression);
    return this;
  }

  @Override
  public void accept(ListenTaskBuilder listenTaskBuilder) {
    listenTaskBuilder.to(
        t -> {
          strategyStep.accept(t);
          if (untilStep != null) {
            untilStep.accept(t);
          }
        });
  }
}
