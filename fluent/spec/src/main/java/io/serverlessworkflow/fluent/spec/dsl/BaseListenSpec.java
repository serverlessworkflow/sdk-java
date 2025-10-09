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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Generic base for Listen specs.
 *
 * <p>Type params: SELF - fluent self type (the concrete spec) LB - ListenTaskBuilder type (e.g.,
 * ListenTaskBuilder, AgentListenTaskBuilder, FuncListenTaskBuilder) TB - ListenToBuilder type
 * (e.g., ListenToBuilder, FuncListenToBuilder) FB - EventFilterBuilder type (e.g.,
 * EventFilterBuilder, FuncEventFilterBuilder) EC - Event configurer type (e.g., EventConfigurer,
 * FuncPredicateEventConfigurer)
 */
public abstract class BaseListenSpec<SELF, LB, TB, FB, EC> {

  @FunctionalInterface
  public interface ToInvoker<LB, TB> {
    void to(LB listenTaskBuilder, Consumer<TB> toStep);
  }

  @FunctionalInterface
  public interface WithApplier<FB, EC> {
    void with(FB filterBuilder, EC eventConfigurer);
  }

  @FunctionalInterface
  public interface FiltersApplier<TB, FB> {
    void apply(TB toBuilder, @SuppressWarnings("rawtypes") Consumer[] filters);
  }

  @FunctionalInterface
  public interface OneFilterApplier<TB, FB> {
    void apply(TB toBuilder, Consumer<FB> filter);
  }

  private final ToInvoker<LB, TB> toInvoker;
  private final WithApplier<FB, EC> withApplier;
  private final FiltersApplier<TB, FB> allApplier;
  private final FiltersApplier<TB, FB> anyApplier;
  private final OneFilterApplier<TB, FB> oneApplier;

  private Consumer<TB> strategyStep;
  private Consumer<TB> untilStep;

  protected BaseListenSpec(
      ToInvoker<LB, TB> toInvoker,
      WithApplier<FB, EC> withApplier,
      FiltersApplier<TB, FB> allApplier,
      FiltersApplier<TB, FB> anyApplier,
      OneFilterApplier<TB, FB> oneApplier) {

    this.toInvoker = Objects.requireNonNull(toInvoker, "toInvoker");
    this.withApplier = Objects.requireNonNull(withApplier, "withApplier");
    this.allApplier = Objects.requireNonNull(allApplier, "allApplier");
    this.anyApplier = Objects.requireNonNull(anyApplier, "anyApplier");
    this.oneApplier = Objects.requireNonNull(oneApplier, "oneApplier");
  }

  protected abstract SELF self();

  protected final void setUntilStep(Consumer<TB> untilStep) {
    this.untilStep = untilStep;
  }

  /** Convert EC[] -> Consumer<FB>[] that call `filterBuilder.with(event)` */
  @SuppressWarnings("unchecked")
  protected final Consumer<FB>[] asFilters(EC... events) {
    Objects.requireNonNull(events, "events");
    Consumer<FB>[] filters = new Consumer[events.length];
    for (int i = 0; i < events.length; i++) {
      EC ev = Objects.requireNonNull(events[i], "events[" + i + "]");
      filters[i] = fb -> withApplier.with(fb, ev);
    }
    return filters;
  }

  @SafeVarargs
  public final SELF all(EC... events) {
    strategyStep = t -> allApplier.apply(t, asFilters(events));
    return self();
  }

  @SafeVarargs
  public final SELF any(EC... events) {
    strategyStep = t -> anyApplier.apply(t, asFilters(events));
    return self();
  }

  public final SELF one(EC event) {
    Objects.requireNonNull(event, "event");
    strategyStep = t -> oneApplier.apply(t, fb -> withApplier.with(fb, event));
    return self();
  }

  /** Concrete 'accept' should delegate here with its concrete LB. */
  protected final void acceptInto(LB listenTaskBuilder) {
    Objects.requireNonNull(strategyStep, "listening strategy must be set (all/any/one)");
    toInvoker.to(
        listenTaskBuilder,
        t -> {
          strategyStep.accept(t);
          if (untilStep != null) {
            untilStep.accept(t);
          }
        });
  }
}
