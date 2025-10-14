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

import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import java.util.function.Consumer;
import java.util.function.Function;

// FuncCallStep
public final class FuncCallStep<T, R> extends Step<FuncCallStep<T, R>, FuncCallTaskBuilder> {
  private final String name; // may be null
  private final Function<T, R> fn;
  private final Class<T> argClass;

  FuncCallStep(Function<T, R> fn, Class<T> argClass) {
    this(null, fn, argClass);
  }

  FuncCallStep(String name, Function<T, R> fn, Class<T> argClass) {
    this.name = name;
    this.fn = fn;
    this.argClass = argClass;
  }

  @Override
  protected void configure(FuncTaskItemListBuilder list, Consumer<FuncCallTaskBuilder> post) {
    if (name == null) {
      list.callFn(
          cb -> {
            cb.function(fn, argClass);
            post.accept(cb);
          });
    } else {
      list.callFn(
          name,
          cb -> {
            cb.function(fn, argClass);
            post.accept(cb);
          });
    }
  }
}
