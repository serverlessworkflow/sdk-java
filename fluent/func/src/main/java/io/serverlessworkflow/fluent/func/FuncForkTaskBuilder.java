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

import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import io.serverlessworkflow.fluent.spec.spi.ForkTaskFluent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class FuncForkTaskBuilder extends TaskBaseBuilder<FuncForkTaskBuilder>
    implements FuncTransformations<FuncForkTaskBuilder>,
        ConditionalTaskBuilder<FuncForkTaskBuilder>,
        ForkTaskFluent<FuncForkTaskBuilder, FuncTaskItemListBuilder> {

  private final ForkTask forkTask;
  private final List<TaskItem> items;

  FuncForkTaskBuilder() {
    this.forkTask = new ForkTask();
    this.forkTask.setFork(new ForkTaskConfiguration());
    this.items = new ArrayList<>();
  }

  @Override
  protected FuncForkTaskBuilder self() {
    return this;
  }

  public <T, V> FuncForkTaskBuilder branch(String name, Function<T, V> function) {
    return branch(name, function, null);
  }

  public <T, V> FuncForkTaskBuilder branch(
      String name, Function<T, V> function, Class<T> argParam) {
    this.items.add(
        new TaskItem(
            name,
            new Task().withCallTask(new CallTaskJava(CallJava.function(function, argParam)))));
    return this;
  }

  public <T, V> FuncForkTaskBuilder branch(Function<T, V> function) {
    return this.branch(UUID.randomUUID().toString(), function);
  }

  @Override
  public FuncForkTaskBuilder branches(Consumer<FuncTaskItemListBuilder> consumer) {
    final FuncTaskItemListBuilder builder = new FuncTaskItemListBuilder();
    consumer.accept(builder);
    this.items.addAll(builder.build());
    return this;
  }

  @Override
  public FuncForkTaskBuilder compete(boolean compete) {
    this.forkTask.getFork().setCompete(compete);
    return this;
  }

  @Override
  public ForkTask build() {
    this.forkTask.getFork().setBranches(this.items);
    return forkTask;
  }
}
