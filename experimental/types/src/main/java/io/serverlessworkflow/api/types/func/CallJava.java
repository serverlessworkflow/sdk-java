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

import io.serverlessworkflow.api.types.TaskBase;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CallJava<T> extends TaskBase {

  private static final long serialVersionUID = 1L;

  private final Optional<Class<T>> inputClass;

  protected CallJava() {
    this(Optional.empty());
  }

  protected CallJava(Optional<Class<T>> inputClass) {
    this.inputClass = inputClass;
  }

  public Optional<Class<T>> inputClass() {
    return inputClass;
  }

  public static <T> CallJava<T> consumer(Consumer<T> consumer) {
    return new CallJavaConsumer<>(consumer, Optional.empty());
  }

  public static <T> CallJava<T> consumer(Consumer<T> consumer, Class<T> inputClass) {
    return new CallJavaConsumer<>(consumer, Optional.ofNullable(inputClass));
  }

  public static <T, V> CallJavaFunction<T, V> function(Function<T, V> function) {
    return new CallJavaFunction<>(function, Optional.empty(), Optional.empty());
  }

  public static <T, V> CallJavaFunction<T, V> function(
      Function<T, V> function, Class<T> inputClass) {
    return new CallJavaFunction<>(function, Optional.ofNullable(inputClass), Optional.empty());
  }

  public static <T, V> CallJavaFunction<T, V> function(
      Function<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return new CallJavaFunction<>(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }

  public static <T, I, V> CallJava<T> loopFunction(
      LoopFunctionIndex<T, I, V> function, String varName, String indexName) {
    return new CallJavaLoopFunctionIndex<>(function, varName, indexName);
  }

  public static <T, I, V> CallJava<T> loopFunction(LoopFunction<T, I, V> function, String varName) {
    return new CallJavaLoopFunction<>(function, varName);
  }

  public static <V, T> CallJava<T> function(ContextFunction<T, V> function, Class<T> inputClass) {
    return new CallJavaContextFunction<>(
        function, Optional.ofNullable(inputClass), Optional.empty());
  }

  public static <V, T> CallJava<T> function(
      ContextFunction<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return new CallJavaContextFunction<>(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }

  public static <V, T> CallJava<T> function(FilterFunction<T, V> function, Class<T> inputClass) {
    return new CallJavaFilterFunction<>(
        function, Optional.ofNullable(inputClass), Optional.empty());
  }

  public static <V, T> CallJava<T> function(
      FilterFunction<T, V> function, Class<T> inputClass, Class<V> outputClass) {
    return new CallJavaFilterFunction<>(
        function, Optional.ofNullable(inputClass), Optional.ofNullable(outputClass));
  }

  public static class CallJavaConsumer<T> extends CallJava<T> {
    private static final long serialVersionUID = 1L;
    private final Consumer<T> consumer;

    public CallJavaConsumer(Consumer<T> consumer, Optional<Class<T>> inputClass) {
      super(inputClass);
      this.consumer = consumer;
    }

    public Consumer<T> consumer() {
      return consumer;
    }
  }

  public static class CallJavaFunction<T, V> extends CallAbstractJavaFunction<T, V> {

    private static final long serialVersionUID = 1L;
    private final Function<T, V> function;

    public CallJavaFunction(
        Function<T, V> function, Optional<Class<T>> inputClass, Optional<Class<V>> outputClass) {
      super(inputClass, outputClass);
      this.function = function;
    }

    public Function<T, V> function() {
      return function;
    }
  }

  public static class CallJavaContextFunction<T, V> extends CallAbstractJavaFunction<T, V> {
    private static final long serialVersionUID = 1L;
    private final ContextFunction<T, V> function;

    public CallJavaContextFunction(
        ContextFunction<T, V> function,
        Optional<Class<T>> inputClass,
        Optional<Class<V>> outputClass) {
      super(inputClass, outputClass);
      this.function = function;
    }

    public ContextFunction<T, V> function() {
      return function;
    }
  }

  public static class CallJavaFilterFunction<T, V> extends CallAbstractJavaFunction<T, V> {
    private static final long serialVersionUID = 1L;
    private final FilterFunction<T, V> function;

    public CallJavaFilterFunction(
        FilterFunction<T, V> function,
        Optional<Class<T>> inputClass,
        Optional<Class<V>> outputClass) {
      super(inputClass, outputClass);
      this.function = function;
    }

    public FilterFunction<T, V> function() {
      return function;
    }
  }

  public static class CallJavaLoopFunction<T, I, V> extends CallAbstractJavaFunction<T, V> {

    private static final long serialVersionUID = 1L;
    private LoopFunction<T, I, V> function;
    private String varName;

    public CallJavaLoopFunction(LoopFunction<T, I, V> function, String varName) {

      this.function = function;
      this.varName = varName;
    }

    public LoopFunction<T, I, V> function() {
      return function;
    }

    public String varName() {
      return varName;
    }
  }

  public static class CallJavaLoopFunctionIndex<T, I, V> extends CallAbstractJavaFunction<T, V> {

    private static final long serialVersionUID = 1L;
    private final LoopFunctionIndex<T, I, V> function;
    private final String varName;
    private final String indexName;

    public CallJavaLoopFunctionIndex(
        LoopFunctionIndex<T, I, V> function, String varName, String indexName) {
      this.function = function;
      this.varName = varName;
      this.indexName = indexName;
    }

    public LoopFunctionIndex<T, I, V> function() {
      return function;
    }

    public String varName() {
      return varName;
    }

    public String indexName() {
      return indexName;
    }
  }
}
