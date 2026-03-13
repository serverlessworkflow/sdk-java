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
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.api.types.func.InputFromFunction;
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
  default <T, V> SELF inputFrom(FilterFunction<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(FilterFunction<T, V> function, Class<T> argClass) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(ContextFunction<T, V> function) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF inputFrom(ContextFunction<T, V> function, Class<T> argClass) {
    setInput(new Input().withFrom(new InputFromFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default SELF inputFrom(String jqExpression) {
    setInput(new Input().withFrom(new InputFrom().withString(jqExpression)));
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
  default <T, V> SELF outputAs(FilterFunction<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(FilterFunction<T, V> function, Class<T> argClass) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(ContextFunction<T, V> function) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default <T, V> SELF outputAs(ContextFunction<T, V> function, Class<T> argClass) {
    setOutput(new Output().withAs(new OutputAsFunction().withFunction(function, argClass)));
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default SELF outputAs(String jqExpression) {
    setOutput(new Output().withAs(new OutputAs().withString(jqExpression)));
    return (SELF) this;
  }
}
