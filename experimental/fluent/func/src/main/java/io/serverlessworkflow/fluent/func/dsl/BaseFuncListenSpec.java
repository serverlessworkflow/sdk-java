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
package io.serverlessworkflow.fluent.func.dsl;

import io.serverlessworkflow.fluent.func.FuncEventFilterBuilder;
import io.serverlessworkflow.fluent.func.FuncListenToBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.spec.dsl.BaseListenSpec;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class BaseFuncListenSpec<SELF, LB>
    extends BaseListenSpec<
        SELF, LB, FuncListenToBuilder, FuncEventFilterBuilder, FuncPredicateEventConfigurer> {

  protected BaseFuncListenSpec(ToInvoker<LB, FuncListenToBuilder> toInvoker) {
    super(
        toInvoker,
        FuncEventFilterBuilder::with,
        // allApplier
        (tb, filters) -> tb.all(castFilters(filters)),
        // anyApplier
        (tb, filters) -> tb.any(castFilters(filters)),
        // oneApplier
        FuncListenToBuilder::one);
  }

  @SuppressWarnings("unchecked")
  private static Consumer<FuncEventFilterBuilder>[] castFilters(Consumer<?>[] arr) {
    return (Consumer<FuncEventFilterBuilder>[]) arr;
  }

  public <T> SELF until(Predicate<T> predicate, Class<T> predClass) {
    Objects.requireNonNull(predicate, "predicate");
    this.setUntilStep(u -> u.until(predicate, predClass));
    return self();
  }
}
