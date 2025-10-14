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
package io.serverlessworkflow.fluent.func.spi;

import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.func.InputFromFunction;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.api.types.func.OutputAsFunction;
import io.serverlessworkflow.fluent.spec.spi.TransformationHandlers;
import java.util.function.Function;

public interface FuncTransformations<SELF extends FuncTransformations<SELF>>
    extends TransformationHandlers {

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(Function<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(Function<T, V> function, Class<T> argClass) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(JavaFilterFunction<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(JavaFilterFunction<T, V> function, Class<T> argClass) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(JavaContextFunction<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(JavaContextFunction<T, V> function, Class<T> argClass) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(Function<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(Function<T, V> function, Class<T> argClass) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(JavaFilterFunction<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(JavaFilterFunction<T, V> function, Class<T> argClass) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(JavaContextFunction<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(JavaContextFunction<T, V> function, Class<T> argClass) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }
}
