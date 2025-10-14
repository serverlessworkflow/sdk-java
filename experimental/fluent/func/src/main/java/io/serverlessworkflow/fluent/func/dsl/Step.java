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

import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** A deferred configurer that can chain when/inputFrom/outputAs/exportAs and apply them later. */
abstract class Step<SELF extends Step<SELF, B>, B> implements FuncTaskConfigurer {

  private final List<Consumer<B>> postConfigurers = new ArrayList<>();

  @SuppressWarnings("unchecked")
  protected final SELF self() {
    return (SELF) this;
  }

  // ---------- ConditionalTaskBuilder passthroughs ----------

  /** Queue a ConditionalTaskBuilder#when(Predicate) to be applied on the concrete builder. */
  public SELF when(Predicate<?> predicate) {
    postConfigurers.add(b -> ((ConditionalTaskBuilder<?>) b).when(predicate));
    return self();
  }

  /** Queue a ConditionalTaskBuilder#when(Predicate, Class) to be applied later. */
  public <T> SELF when(Predicate<T> predicate, Class<T> argClass) {
    postConfigurers.add(b -> ((ConditionalTaskBuilder<?>) b).when(predicate, argClass));
    return self();
  }

  // ---------- FuncTaskTransformations passthroughs: exportAs ----------

  /** Queue a FuncTaskTransformations#exportAs(Function) to be applied later. */
  public <T, V> SELF exportAs(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#exportAs(Function, Class) to be applied later. */
  public <T, V> SELF exportAs(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#exportAs(JavaFilterFunction) to be applied later. */
  public <T, V> SELF exportAs(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#exportAs(JavaFilterFunction, Class) to be applied later. */
  public <T, V> SELF exportAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#exportAs(JavaContextFunction) to be applied later. */
  public <T, V> SELF exportAs(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#exportAs(JavaContextFunction, Class) to be applied later. */
  public <T, V> SELF exportAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  // ---------- FuncTaskTransformations passthroughs: outputAs ----------

  /** Queue a FuncTaskTransformations#outputAs(Function) to be applied later. */
  public <T, V> SELF outputAs(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#outputAs(Function, Class) to be applied later. */
  public <T, V> SELF outputAs(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#outputAs(JavaFilterFunction) to be applied later. */
  public <T, V> SELF outputAs(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#outputAs(JavaFilterFunction, Class) to be applied later. */
  public <T, V> SELF outputAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#outputAs(JavaContextFunction) to be applied later. */
  public <T, V> SELF outputAs(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#outputAs(JavaContextFunction, Class) to be applied later. */
  public <T, V> SELF outputAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  // ---------- FuncTaskTransformations passthroughs: inputFrom ----------

  /** Queue a FuncTaskTransformations#inputFrom(Function) to be applied later. */
  public <T, V> SELF inputFrom(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#inputFrom(Function, Class) to be applied later. */
  public <T, V> SELF inputFrom(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#inputFrom(JavaFilterFunction) to be applied later. */
  public <T, V> SELF inputFrom(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#inputFrom(JavaFilterFunction, Class) to be applied later. */
  public <T, V> SELF inputFrom(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  /** Queue a FuncTaskTransformations#inputFrom(JavaContextFunction) to be applied later. */
  public <T, V> SELF inputFrom(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue a FuncTaskTransformations#inputFrom(JavaContextFunction, Class) to be applied later. */
  public <T, V> SELF inputFrom(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  // ---------- wiring into the underlying list/builder ----------

  @Override
  public final void accept(FuncTaskItemListBuilder list) {
    configure(list, this::applyPost);
  }

  /** Implement per-step to attach to the correct builder and run {@code post} at the end. */
  protected abstract void configure(FuncTaskItemListBuilder list, Consumer<B> post);

  /** Applies all queued post-configurers to the concrete builder. */
  private void applyPost(B builder) {
    for (Consumer<B> c : postConfigurers) c.accept(builder);
  }
}
