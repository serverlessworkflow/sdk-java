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

public final class ConsumeStep<T> extends Step<ConsumeStep<T>, FuncCallTaskBuilder> {
  private final String name; // may be null
  private final Consumer<T> consumer;
  private final Class<T> argClass; // may be null

  ConsumeStep(Consumer<T> consumer, Class<T> argClass) {
    this(null, consumer, argClass);
  }

  ConsumeStep(String name, Consumer<T> consumer, Class<T> argClass) {
    this.name = name;
    this.consumer = consumer;
    this.argClass = argClass;
  }

  @Override
  protected void configure(
      FuncTaskItemListBuilder list, java.util.function.Consumer<FuncCallTaskBuilder> post) {
    if (name == null) {
      list.callFn(
          cb -> {
            // prefer the typed consumer if your builder supports it; otherwise fallback:
            if (argClass != null) cb.consumer(consumer, argClass);
            else cb.consumer(consumer);
            post.accept(cb);
          });
    } else {
      list.callFn(
          name,
          cb -> {
            if (argClass != null) cb.consumer(consumer, argClass);
            else cb.consumer(consumer);
            post.accept(cb);
          });
    }
  }
}
