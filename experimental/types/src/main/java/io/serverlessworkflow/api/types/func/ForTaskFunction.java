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
import java.util.function.Function;

public class ForTaskFunction extends ForTask {

  private static final long serialVersionUID = 1L;
  private LoopPredicateIndex<?, ?> whilePredicate;
  private Function<?, Collection<?>> collection;

  public <T, V> ForTaskFunction withWhile(LoopPredicate<T, V> whilePredicate) {
    this.whilePredicate = toPredicateIndex(whilePredicate);
    return this;
  }

  private <T, V> LoopPredicateIndex<T, V> toPredicateIndex(LoopPredicate<T, V> whilePredicate) {
    return (model, item, index) -> whilePredicate.test(model, item);
  }

  public <T, V> ForTaskFunction withWhile(LoopPredicateIndex<T, V> whilePredicate) {
    this.whilePredicate = whilePredicate;
    return this;
  }

  public <T> ForTaskFunction withCollection(Function<T, Collection<?>> collection) {
    this.collection = collection;
    return this;
  }

  public LoopPredicateIndex<?, ?> getWhilePredicate() {
    return whilePredicate;
  }

  public Function<?, Collection<?>> getCollection() {
    return collection;
  }
}
