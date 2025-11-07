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
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import java.util.function.Consumer;
import java.util.function.Function;

public final class FuncCallStep<T, R> extends Step<FuncCallStep<T, R>, FuncCallTaskBuilder> {

  private final String name;
  private final Function<T, R> fn;
  private final JavaContextFunction<T, R> ctxFn;
  private final JavaFilterFunction<T, R> filterFn;
  private final Class<T> argClass;

  /** Function<T,R> variant (unnamed). */
  FuncCallStep(Function<T, R> fn, Class<T> argClass) {
    this(null, fn, argClass);
  }

  /** Function<T,R> variant (named). */
  FuncCallStep(String name, Function<T, R> fn, Class<T> argClass) {
    this.name = name;
    this.fn = fn;
    this.ctxFn = null;
    this.filterFn = null;
    this.argClass = argClass;
  }

  /** JavaContextFunction<T,R> variant (unnamed). */
  FuncCallStep(JavaContextFunction<T, R> ctxFn, Class<T> argClass) {
    this(null, ctxFn, argClass);
  }

  /** JavaContextFunction<T,R> variant (named). */
  FuncCallStep(String name, JavaContextFunction<T, R> ctxFn, Class<T> argClass) {
    this.name = name;
    this.fn = null;
    this.ctxFn = ctxFn;
    this.filterFn = null;
    this.argClass = argClass;
  }

  /** JavaFilterFunction<T,R> variant (unnamed). */
  FuncCallStep(JavaFilterFunction<T, R> filterFn, Class<T> argClass) {
    this(null, filterFn, argClass);
  }

  /** JavaFilterFunction<T,R> variant (named). */
  FuncCallStep(String name, JavaFilterFunction<T, R> filterFn, Class<T> argClass) {
    this.name = name;
    this.fn = null;
    this.ctxFn = null;
    this.filterFn = filterFn;
    this.argClass = argClass;
  }

  @Override
  protected void configure(FuncTaskItemListBuilder list, Consumer<FuncCallTaskBuilder> post) {
    final Consumer<FuncCallTaskBuilder> apply =
        cb -> {
          if (ctxFn != null) {
            cb.function(ctxFn, argClass);
          } else if (filterFn != null) {
            cb.function(filterFn, argClass);
          } else {
            cb.function(fn, argClass);
          }
          post.accept(cb);
        };

    if (name == null) {
      list.callFn(apply);
    } else {
      list.callFn(name, apply);
    }
  }
}
