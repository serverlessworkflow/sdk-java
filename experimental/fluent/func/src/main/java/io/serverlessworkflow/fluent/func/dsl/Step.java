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

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A deferred configurer that can chain when/inputFrom/outputAs/exportAs and apply them later to the
 * concrete task builder (e.g., Call/Emit/Listen builder).
 */
abstract class Step<SELF extends Step<SELF, B>, B> implements FuncTaskConfigurer {

  private final List<Consumer<B>> postConfigurers = new ArrayList<>();

  @SuppressWarnings("unchecked")
  protected SELF self() {
    return (SELF) this;
  }

  // ---------------------------------------------------------------------------
  // ConditionalTaskBuilder passthroughs (if/when)
  // ---------------------------------------------------------------------------

  /** Queue a {@code when(predicate)} to be applied on the concrete builder. */
  public SELF when(Predicate<?> predicate) {
    postConfigurers.add(b -> ((ConditionalTaskBuilder<?>) b).when(predicate));
    return self();
  }

  /** Queue a {@code when(predicate, argClass)} to be applied on the concrete builder. */
  public <T> SELF when(Predicate<T> predicate, Class<T> argClass) {
    postConfigurers.add(b -> ((ConditionalTaskBuilder<?>) b).when(predicate, argClass));
    return self();
  }

  public SELF when(String jqExpr) {
    postConfigurers.add(b -> ((TaskBaseBuilder<?>) b).when(jqExpr));
    return self();
  }

  /**
   * Queue a {@code then(taskName)} to be applied on the concrete builder. Directs the workflow
   * engine to jump to the named task after this one completes.
   *
   * @param taskName the name of the next task to execute
   * @return this step for further chaining
   * @see <a
   *     href="https://github.com/serverlessworkflow/specification/blob/main/dsl-reference.md#task">DSL
   *     Reference - Task</a>
   */
  public SELF then(String taskName) {
    postConfigurers.add(b -> ((TaskBaseBuilder<?>) b).then(taskName));
    return self();
  }

  /**
   * Queue a {@code then(directive)} to be applied on the concrete builder. Directs the workflow
   * engine to apply the given flow directive after this task completes.
   *
   * @param directive the flow directive (e.g., {@link FlowDirectiveEnum#END})
   * @return this step for further chaining
   * @see <a
   *     href="https://github.com/serverlessworkflow/specification/blob/main/dsl-reference.md#task">DSL
   *     Reference - Task</a>
   */
  public SELF then(FlowDirectiveEnum directive) {
    postConfigurers.add(b -> ((TaskBaseBuilder<?>) b).then(directive));
    return self();
  }

  // ---------------------------------------------------------------------------
  // FuncTaskTransformations passthroughs: EXPORT (fn/context/filter + JQ)
  // ---------------------------------------------------------------------------

  /** Queue {@code exportAs(fn)} to be applied later. */
  public <T, V> SELF exportAs(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue {@code exportAs(fn, argClass)} to be applied later. */
  public <T, V> SELF exportAs(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  /** Queue {@code exportAs(filterFn)} to be applied later. */
  public <T, V> SELF exportAs(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue {@code exportAs(filterFn, argClass)} to be applied later. */
  public <T, V> SELF exportAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  /** Queue {@code exportAs(ctxFn)} to be applied later. */
  public <T, V> SELF exportAs(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function));
    return self();
  }

  /** Queue {@code exportAs(ctxFn, argClass)} to be applied later. */
  public <T, V> SELF exportAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).exportAs(function, argClass));
    return self();
  }

  /**
   * Queue {@code exportAs(jqExpression)} (JQ string) to be applied later. Example: {@code
   * exportAs(FuncDSL.selectFirstStringify())}
   */
  public SELF exportAs(String jqExpression) {
    postConfigurers.add(b -> ((TaskBaseBuilder<?>) b).exportAs(jqExpression));
    return self();
  }

  // ---------------------------------------------------------------------------
  // FuncTaskTransformations passthroughs: OUTPUT (fn/context/filter + JQ)
  // ---------------------------------------------------------------------------

  /** Queue {@code outputAs(fn)} to be applied later. */
  public <T, V> SELF outputAs(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue {@code outputAs(fn, argClass)} to be applied later. */
  public <T, V> SELF outputAs(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  /** Queue {@code outputAs(filterFn)} to be applied later. */
  public <T, V> SELF outputAs(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue {@code outputAs(filterFn, argClass)} to be applied later. */
  public <T, V> SELF outputAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  /** Queue {@code outputAs(ctxFn)} to be applied later. */
  public <T, V> SELF outputAs(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function));
    return self();
  }

  /** Queue {@code outputAs(ctxFn, argClass)} to be applied later. */
  public <T, V> SELF outputAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(function, argClass));
    return self();
  }

  /**
   * Queue {@code outputAs(jqExpression)} (JQ string) to be applied later. Example: {@code
   * outputAs(FuncDSL.selectFirstStringify())}
   */
  public SELF outputAs(String jqExpression) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).outputAs(jqExpression));
    return self();
  }

  // ---------------------------------------------------------------------------
  // FuncTaskTransformations passthroughs: INPUT (fn/context/filter + JQ)
  // ---------------------------------------------------------------------------

  /** Queue {@code inputFrom(fn)} to be applied later. */
  public <T, V> SELF inputFrom(Function<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue {@code inputFrom(fn, argClass)} to be applied later. */
  public <T, V> SELF inputFrom(Function<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  /** Queue {@code inputFrom(filterFn)} to be applied later. */
  public <T, V> SELF inputFrom(JavaFilterFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue {@code inputFrom(filterFn, argClass)} to be applied later. */
  public <T, V> SELF inputFrom(JavaFilterFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  /** Queue {@code inputFrom(ctxFn)} to be applied later. */
  public <T, V> SELF inputFrom(JavaContextFunction<T, V> function) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function));
    return self();
  }

  /** Queue {@code inputFrom(ctxFn, argClass)} to be applied later. */
  public <T, V> SELF inputFrom(JavaContextFunction<T, V> function, Class<T> argClass) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(function, argClass));
    return self();
  }

  /**
   * Queue {@code inputFrom(jqExpression)} (JQ string) to be applied later. Example: {@code
   * inputFrom(".payload")}
   */
  public SELF inputFrom(String jqExpression) {
    postConfigurers.add(b -> ((FuncTaskTransformations<?>) b).inputFrom(jqExpression));
    return self();
  }

  // ---------------------------------------------------------------------------
  // wiring into the underlying list/builder
  // ---------------------------------------------------------------------------

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
