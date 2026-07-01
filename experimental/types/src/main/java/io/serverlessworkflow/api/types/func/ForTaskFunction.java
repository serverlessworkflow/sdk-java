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

import io.serverlessworkflow.api.reflection.func.ReflectionUtils;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.TaskMetadata;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class ForTaskFunction {

  private final ForTask forTask;
  private TaskMetadata metadata;

  public static final String WHILE_PREDICATE = "whilePredicate";
  public static final String WHILE_CLASS = "whileClass";
  public static final String ITEM_CLASS = "itemClass";
  public static final String FOR_CLASS = "forClass";
  public static final String COLLECTION = "inCollection";

  public ForTaskFunction(ForTask forTask) {
    this.forTask = forTask;
    this.metadata = forTask.getMetadata();
    if (metadata == null) {
      metadata = new TaskMetadata();
      forTask.setMetadata(metadata);
    }
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicate<T, V> whilePredicate) {
    return withWhile(toPredicate(whilePredicate));
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicate<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(toPredicate(whilePredicate), modelClass);
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicate<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(toPredicate(whilePredicate), modelClass, itemClass);
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicateIndex<T, V> whilePredicate) {
    return withWhile(toPredicate(whilePredicate), Optional.empty(), Optional.empty());
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndex<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(
        toPredicate(whilePredicate), Optional.ofNullable(modelClass), Optional.empty());
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndex<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(
        toPredicate(whilePredicate),
        Optional.ofNullable(modelClass),
        Optional.ofNullable(itemClass));
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicateIndexContext<T, V> whilePredicate) {
    Optional<MethodType> methodType = ReflectionUtils.methodType(whilePredicate);
    return withWhile(
        toPredicate(whilePredicate),
        methodType.map(m -> m.parameterType(0)),
        methodType.map(MethodType::returnType));
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndexContext<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(
        toPredicate(whilePredicate),
        Optional.ofNullable(modelClass),
        ReflectionUtils.methodType(whilePredicate).map(MethodType::returnType));
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndexContext<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(
        toPredicate(whilePredicate),
        Optional.ofNullable(modelClass),
        Optional.ofNullable(itemClass));
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicateIndexFilter<T, V> whilePredicate) {
    Optional<MethodType> methodType = ReflectionUtils.methodType(whilePredicate);
    return withWhile(
        whilePredicate,
        methodType.map(m -> m.parameterType(0)),
        methodType.map(MethodType::returnType));
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndexFilter<T, V> whilePredicate, Class<T> modelClass) {
    return withWhile(
        whilePredicate,
        Optional.ofNullable(modelClass),
        ReflectionUtils.methodType(whilePredicate).map(MethodType::returnType));
  }

  public <T, V> ForTaskFunction withWhile(
      LoopPredicateIndexFilter<T, V> whilePredicate, Class<T> modelClass, Class<V> itemClass) {
    return withWhile(
        whilePredicate, Optional.ofNullable(modelClass), Optional.ofNullable(itemClass));
  }

  private <T, V> LoopPredicateIndexFilter<T, V> toPredicate(LoopPredicate<T, V> whilePredicate) {
    return (model, item, index, w, t) -> whilePredicate.test(model, item);
  }

  private <T, V> LoopPredicateIndexFilter<T, V> toPredicate(
      LoopPredicateIndex<T, V> whilePredicate) {
    return (model, item, index, w, t) -> whilePredicate.test(model, item, index);
  }

  private <T, V> LoopPredicateIndexFilter<T, V> toPredicate(
      LoopPredicateIndexContext<T, V> whilePredicate) {
    return (model, item, index, w, t) -> whilePredicate.test(model, item, index, w);
  }

  private <T, V> ForTaskFunction withWhile(
      LoopPredicateIndexFilter<T, V> whilePredicate,
      Optional<Class<?>> modelClass,
      Optional<Class<?>> itemClass) {
    metadata.withAdditionalProperty(WHILE_PREDICATE, whilePredicate);
    metadata.withAdditionalProperty(WHILE_CLASS, modelClass);
    metadata.withAdditionalProperty(ITEM_CLASS, itemClass);
    return this;
  }

  public <T, V> ForTaskFunction withCollection(Function<T, Collection<V>> collection) {
    return withCollection(collection, null);
  }

  public <T, V> ForTaskFunction withCollection(
      Function<T, Collection<V>> collection, Class<T> colArgClass) {
    metadata.withAdditionalProperty(COLLECTION, collection);
    metadata.withAdditionalProperty(FOR_CLASS, Optional.ofNullable(colArgClass));
    ForTaskConfiguration forConfig = forTask.getFor();
    if (forConfig == null) {
      forConfig = new ForTaskConfiguration();
      forTask.setFor(forConfig);
    }
    if (forConfig.getIn() == null) {
      forConfig.setIn("Handling item collection with metadata key " + ForTaskFunction.COLLECTION);
    }
    return this;
  }

  public LoopPredicateIndexFilter<?, ?> getWhilePredicate() {
    return (LoopPredicateIndexFilter<?, ?>) metadata.getAdditionalProperties().get(WHILE_PREDICATE);
  }

  public Optional<Class<?>> getWhileClass() {
    return (Optional<Class<?>>)
        metadata.getAdditionalProperties().getOrDefault(WHILE_CLASS, Optional.empty());
  }

  public Optional<Class<?>> getForClass() {
    return (Optional<Class<?>>)
        metadata.getAdditionalProperties().getOrDefault(FOR_CLASS, Optional.empty());
  }

  public Optional<Class<?>> getItemClass() {
    return (Optional<Class<?>>)
        metadata.getAdditionalProperties().getOrDefault(ITEM_CLASS, Optional.empty());
  }

  public Function<?, Collection<?>> getInCollection() {
    return (Function<?, Collection<?>>) metadata.getAdditionalProperties().get(COLLECTION);
  }

  public ForTask task() {
    return forTask;
  }
}
