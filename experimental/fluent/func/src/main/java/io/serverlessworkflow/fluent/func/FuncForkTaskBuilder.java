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

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.AbstractForkTaskBuilder;
import io.serverlessworkflow.fluent.spec.spi.ForkTaskFluent;
import java.util.function.Function;

public class FuncForkTaskBuilder
    extends AbstractForkTaskBuilder<FuncForkTaskBuilder, FuncTaskItemListBuilder>
    implements FuncTaskTransformations<FuncForkTaskBuilder>,
        ConditionalTaskBuilder<FuncForkTaskBuilder>,
        ForkTaskFluent<FuncForkTaskBuilder, FuncTaskItemListBuilder> {

  FuncForkTaskBuilder() {
    super();
  }

  @Override
  protected FuncForkTaskBuilder self() {
    return this;
  }

  @Override
  protected FuncTaskItemListBuilder newTaskItemListBuilder(int listOffsetSize) {
    return new FuncTaskItemListBuilder(listOffsetSize);
  }

  public <T, V> FuncForkTaskBuilder branch(String name, Function<T, V> function) {
    return branch(name, function, null);
  }

  public <T, V> FuncForkTaskBuilder branch(
      String name, Function<T, V> function, Class<T> argParam) {
    return branch(name, function, argParam, null);
  }

  public <T, V> FuncForkTaskBuilder branch(
      String name, Function<T, V> function, Class<T> argParam, Class<V> returnClass) {
    this.appendBranch(
        new TaskItem(
            this.defaultBranchName(name, this.currentOffset()),
            new Task()
                .withCallTask(
                    new CallTaskJava(CallJava.function(function, argParam, returnClass)))));
    return this;
  }

  public <T, V> FuncForkTaskBuilder branch(Function<T, V> function) {
    return this.branch(null, function);
  }
}
