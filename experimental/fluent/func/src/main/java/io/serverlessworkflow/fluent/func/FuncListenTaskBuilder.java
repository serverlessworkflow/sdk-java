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

import io.serverlessworkflow.api.types.utils.TaskPredicate;
import io.serverlessworkflow.api.types.utils.TypesUtils;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.AbstractListenTaskBuilder;
import java.util.function.Predicate;

public class FuncListenTaskBuilder
    extends AbstractListenTaskBuilder<FuncTaskItemListBuilder, FuncListenToBuilder>
    implements ConditionalTaskBuilder<FuncListenTaskBuilder>,
        FuncTaskTransformations<FuncListenTaskBuilder> {

  FuncListenTaskBuilder(FuncTaskItemListBuilder factory) {
    super(factory);
  }

  public <T> FuncListenTaskBuilder until(Predicate<T> predicate, Class<T> predClass) {
    TaskPredicate.withPredicate(
        super.getListenTask(), TypesUtils.UNTIL_PRED_NAME, predicate, predClass);
    return this;
  }

  @Override
  protected FuncListenTaskBuilder self() {
    return this;
  }

  @Override
  protected FuncListenToBuilder newEventConsumptionStrategyBuilder() {
    return new FuncListenToBuilder(super.getListenTask());
  }
}
