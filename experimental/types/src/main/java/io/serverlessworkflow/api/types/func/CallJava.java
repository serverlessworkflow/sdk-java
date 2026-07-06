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
import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.FunctionArguments;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CallJava {

  private CallJava() {}

  public static final String JAVA_CALL_KEY = "Java";
  public static final String FUNCTION_NAME_KEY = "function";
  public static final String INPUT_CLASS_KEY = "inputClass";
  public static final String OUTPUT_CLASS_KEY = "outputClass";
  public static final String VAR_NAME_KEY = "varName";
  public static final String INDEX_NAME_KEY = "index";

  private static CallFunction buildFunction(
      Object function, Optional<Class<?>> inputClass, Optional<Class<?>> outputClass) {
    CallFunction result = new CallFunction();
    result.setCall(JAVA_CALL_KEY);
    result.withWith(
        new FunctionArguments()
            .withAdditionalProperty(FUNCTION_NAME_KEY, function)
            .withAdditionalProperty(INPUT_CLASS_KEY, inputClass)
            .withAdditionalProperty(OUTPUT_CLASS_KEY, outputClass));
    return result;
  }

  public static <T> CallFunction consumer(Consumer<T> consumer) {
    return buildFunction(
        consumer,
        ReflectionUtils.methodType(consumer).map(m -> m.parameterType(0)),
        Optional.empty());
  }

  public static <T> CallFunction consumer(Consumer<T> consumer, Class<T> inputClass) {
    return buildFunction(consumer, Optional.ofNullable(inputClass), Optional.empty());
  }

  public static <T, V> CallFunction function(Function<T, V> function) {
    return buildFunction(function, Optional.empty(), Optional.empty());
  }

  public static <T, V> CallFunction function(Function<T, V> function, Class<T> inputClass) {
    return buildFunction(
        function,
        Optional.ofNullable(inputClass),
        ReflectionUtils.methodType(function).map(MethodType::returnType));
  }

  public static <T, V> CallFunction function(
      Function<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return buildFunction(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }

  public static <T, I, V> CallFunction loopFunction(
      LoopFunctionIndex<T, I, V> function, String varName, String indexName) {
    Optional<MethodType> methodType = ReflectionUtils.methodType(function);
    CallFunction result =
        buildFunction(
            function,
            methodType.map(m -> m.parameterType(0)),
            methodType.map(MethodType::returnType));
    result
        .getWith()
        .withAdditionalProperty(VAR_NAME_KEY, varName)
        .withAdditionalProperty(INDEX_NAME_KEY, indexName);
    return result;
  }

  public static <T, I, V> CallFunction loopFunction(
      LoopFunction<T, I, V> function, String varName) {
    Optional<MethodType> methodType = ReflectionUtils.methodType(function);
    CallFunction result =
        buildFunction(
            function,
            methodType.map(m -> m.parameterType(0)),
            methodType.map(MethodType::returnType));
    result.getWith().withAdditionalProperty(VAR_NAME_KEY, varName);
    return result;
  }

  public static <V, T> CallFunction function(ContextFunction<T, V> function, Class<T> inputClass) {
    return buildFunction(
        function,
        Optional.ofNullable(inputClass),
        ReflectionUtils.methodType(function).map(MethodType::returnType));
  }

  public static <V, T> CallFunction function(
      ContextFunction<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return buildFunction(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }

  public static <V, T> CallFunction function(FilterFunction<T, V> function, Class<T> inputClass) {
    return buildFunction(
        function,
        Optional.ofNullable(inputClass),
        ReflectionUtils.methodType(function).map(MethodType::returnType));
  }

  public static <V, T> CallFunction function(
      FilterFunction<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return buildFunction(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }
}
