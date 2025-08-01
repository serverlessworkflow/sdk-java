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
package io.serverlessworkflow.api.types.func;

import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.impl.expressions.LoopPredicate;
import io.serverlessworkflow.impl.expressions.LoopPredicateIndex;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class ForTaskFunction extends ForTask {

  private static final long serialVersionUID = 1L;
  private LoopPredicateIndex<?, ?> whilePredicate;
  private Optional<Class<?>> modelClass;
  private Optional<Class<?>> itemClass;
  private Function<?, Collection<?>> collection;

  public <T, V> ForTaskFunction withWhile(LoopPredicate<T, V> whilePredicate) {
    return withWhile(toPredicateIndex(whilePredicate));
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicate<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(toPredicateIndex(whilePredicate), modelClass);
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicate<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(toPredicateIndex(whilePredicate), modelClass, itemClass);
  }

  private <T, V> LoopPredicateIndex<T, V> toPredicateIndex(LoopPredicate<T, V> whilePredicate) {
    return (model, item, index) -> whilePredicate.test(model, item);
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicateIndex<T, V> whilePredicate) {
    return withWhile(whilePredicate, Optional.empty(), Optional.empty());
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndex<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(whilePredicate, Optional.of(modelClass), Optional.empty());
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndex<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(whilePredicate, Optional.of(modelClass), Optional.of(itemClass));
  }

  private <T, V> ForTaskFunction withWhile(
      LoopPredicateIndex<T, V> whilePredicate,
      Optional<Class<?>> modelClass,
      Optional<Class<?>> itemClass) {
    this.whilePredicate = whilePredicate;
    this.modelClass = modelClass;
    this.itemClass = itemClass;
    return this;
  }

  public <T> ForTaskFunction withCollection(Function<T, Collection<?>> collection) {
    this.collection = collection;
    return this;
  }

  public LoopPredicateIndex<?, ?> getWhilePredicate() {
    return whilePredicate;
  }

  public Optional<Class<?>> getModelClass() {
    return modelClass;
  }

  public Optional<Class<?>> getItemClass() {
    return itemClass;
  }

  public Function<?, Collection<?>> getCollection() {
    return collection;
  }
}
