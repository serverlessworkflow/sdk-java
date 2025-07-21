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
package io.serverlessworkflow.fluent.java;

import io.serverlessworkflow.api.types.CallJava;
import io.serverlessworkflow.api.types.CallTaskJava;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.ForTaskFunction;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.standard.TaskBaseBuilder;
import io.serverlessworkflow.impl.expressions.LoopFunction;
import io.serverlessworkflow.impl.expressions.LoopPredicate;
import io.serverlessworkflow.impl.expressions.LoopPredicateIndex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class ForTaskJavaBuilder extends TaskBaseBuilder<ForTaskJavaBuilder>
    implements JavaTransformationHandlers<ForTaskJavaBuilder> {

  private final ForTaskFunction forTaskFunction;
  private final List<TaskItem> items;

  ForTaskJavaBuilder() {
    this.forTaskFunction = new ForTaskFunction();
    this.forTaskFunction.withFor(new ForTaskConfiguration());
    this.items = new ArrayList<>();
    super.setTask(forTaskFunction);
  }

  @Override
  protected ForTaskJavaBuilder self() {
    return this;
  }

  public <T, V> ForTaskJavaBuilder whileC(LoopPredicate<T, V> predicate) {
    this.forTaskFunction.withWhile(predicate);
    return this;
  }

  public <T, V> ForTaskJavaBuilder whileC(LoopPredicateIndex<T, V> predicate) {
    this.forTaskFunction.withWhile(predicate);
    return this;
  }

  public <T> ForTaskJavaBuilder collection(Function<T, Collection<?>> collectionF) {
    this.forTaskFunction.withCollection(collectionF);
    return this;
  }

  public <T, V, R> ForTaskJavaBuilder tasks(String name, LoopFunction<T, V, R> function) {
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

  public <T, V, R> ForTaskJavaBuilder tasks(LoopFunction<T, V, R> function) {
    return this.tasks(UUID.randomUUID().toString(), function);
  }

  public ForTaskJavaBuilder tasks(Consumer<TaskItemListJavaBuilder> consumer) {
    final TaskItemListJavaBuilder builder = new TaskItemListJavaBuilder();
    consumer.accept(builder);
    this.items.addAll(builder.build());
    return this;
  }

  public ForTaskFunction build() {
    this.forTaskFunction.setDo(this.items);
    return this.forTaskFunction;
  }
}
