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

import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import io.serverlessworkflow.impl.expressions.LoopFunction;
import io.serverlessworkflow.impl.expressions.LoopPredicate;
import io.serverlessworkflow.impl.expressions.LoopPredicateIndex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class FuncForTaskBuilder extends TaskBaseBuilder<FuncForTaskBuilder>
    implements FuncTransformations<FuncForTaskBuilder> {

  private final ForTaskFunction forTaskFunction;
  private final List<TaskItem> items;

  FuncForTaskBuilder() {
    this.forTaskFunction = new ForTaskFunction();
    this.forTaskFunction.withFor(new ForTaskConfiguration());
    this.items = new ArrayList<>();
    super.setTask(forTaskFunction);
  }

  @Override
  protected FuncForTaskBuilder self() {
    return this;
  }

  public <T, V> FuncForTaskBuilder whileC(LoopPredicate<T, V> predicate) {
    this.forTaskFunction.withWhile(predicate);
    return this;
  }

  public <T, V> FuncForTaskBuilder whileC(LoopPredicateIndex<T, V> predicate) {
    this.forTaskFunction.withWhile(predicate);
    return this;
  }

  public <T> FuncForTaskBuilder collection(Function<T, Collection<?>> collectionF) {
    this.forTaskFunction.withCollection(collectionF);
    return this;
  }

  public <T, V, R> FuncForTaskBuilder tasks(String name, LoopFunction<T, V, R> function) {
    this.items.add(
        new TaskItem(
            name,
            new Task()
                .withCallTask(
                    new CallTaskJava(
                        CallJava.loopFunction(
                            function, this.forTaskFunction.getFor().getEach())))));
    return this;
  }

  public <T, V, R> FuncForTaskBuilder tasks(LoopFunction<T, V, R> function) {
    return this.tasks(UUID.randomUUID().toString(), function);
  }

  public FuncForTaskBuilder tasks(Consumer<FuncTaskItemListBuilder> consumer) {
    final FuncTaskItemListBuilder builder = new FuncTaskItemListBuilder();
    consumer.accept(builder);
    this.items.addAll(builder.build());
    return this;
  }

  public ForTaskFunction build() {
    this.forTaskFunction.setDo(this.items);
    return this.forTaskFunction;
  }
}
