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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.api.types.AnyEventConsumptionStrategy;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.func.UntilPredicate;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTransformations;
import io.serverlessworkflow.fluent.spec.BaseDoTaskBuilder;
import io.serverlessworkflow.fluent.spec.BaseTaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.ListenTaskBuilder;
import java.util.function.Predicate;

public class FuncListenTaskBuilder extends ListenTaskBuilder<FuncTaskItemListBuilder>
    implements ConditionalTaskBuilder<FuncListenTaskBuilder>,
        FuncTransformations<FuncListenTaskBuilder> {

  private UntilPredicate untilPredicate;

  FuncListenTaskBuilder() {
    super(new FuncTaskItemListBuilder());
  }

  public <T> FuncListenTaskBuilder until(Predicate<T> predicate, Class<T> predClass) {
    untilPredicate = new UntilPredicate().withPredicate(predicate, predClass);
    return this;
  }

  @Override
  public ListenTask build() {
    ListenTask task = super.build();
    AnyEventConsumptionStrategy anyEvent =
        task.getListen().getTo().getAnyEventConsumptionStrategy();
    if (untilPredicate != null && anyEvent != null) {
      anyEvent.withUntil(untilPredicate);
    }
    return task;
  }
}
