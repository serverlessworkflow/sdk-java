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

import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FuncCallStep<T, R> extends Step<FuncCallStep<T, R>, FuncCallTaskBuilder> {

  private final String name;
  private final Function<T, R> fn;
  private final ContextFunction<T, R> ctxFn;
  private final FilterFunction<T, R> filterFn;
  private final Class<T> argClass;
  private final Class<R> returnClass;

  /** Function<T,R> variant (unnamed). */
  FuncCallStep(Function<T, R> fn, Class<T> argClass, Class<R> returnClass) {
    this(null, fn, argClass, returnClass);
  }

  /** Function<T,R> variant (named). */
  FuncCallStep(String name, Function<T, R> fn, Class<T> argClass, Class<R> returnClass) {
    this.name = name;
    this.fn = fn;
    this.ctxFn = null;
    this.filterFn = null;
    this.argClass = argClass;
    this.returnClass = returnClass;
  }

  /** ContextFunction<T,R> variant (unnamed). */
  FuncCallStep(ContextFunction<T, R> ctxFn, Class<T> argClass, Class<R> returnClass) {
    this(null, ctxFn, argClass, returnClass);
  }

  /** ContextFunction<T,R> variant (named). */
  FuncCallStep(String name, ContextFunction<T, R> ctxFn, Class<T> argClass, Class<R> returnClass) {
    this.name = name;
    this.fn = null;
    this.ctxFn = ctxFn;
    this.filterFn = null;
    this.argClass = argClass;
    this.returnClass = returnClass;
  }

  /** FilterFunction<T,R> variant (unnamed). */
  FuncCallStep(FilterFunction<T, R> filterFn, Class<T> argClass, Class<R> returnClass) {
    this(null, filterFn, argClass, returnClass);
  }

  /** FilterFunction<T,R> variant (named). */
  FuncCallStep(
      String name, FilterFunction<T, R> filterFn, Class<T> argClass, Class<R> returnClass) {
    this.name = name;
    this.fn = null;
    this.ctxFn = null;
    this.filterFn = filterFn;
    this.argClass = argClass;
    this.returnClass = returnClass;
  }

  @Override
  protected void configure(FuncTaskItemListBuilder list, Consumer<FuncCallTaskBuilder> post) {
    final Consumer<FuncCallTaskBuilder> apply =
        cb -> {
          if (ctxFn != null) {
            cb.function(ctxFn, argClass, returnClass);
          } else if (filterFn != null) {
            cb.function(filterFn, argClass, returnClass);
          } else {
            cb.function(fn, argClass, returnClass);
          }
          post.accept(cb);
        };

    if (name == null) {
      list.function(apply);
    } else {
      list.function(name, apply);
    }
  }
}
