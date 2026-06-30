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
package io.serverlessworkflow.api.reflection.func;

import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specially used by {@link Function} parameters in the Java Function.
 *
 * @see <a href="https://www.baeldung.com/java-serialize-lambda">Serialize a Lambda in Java</a>
 */
public final class ReflectionUtils {

  private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

  private ReflectionUtils() {}

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(ContextFunction<T, ?> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 0);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(FilterFunction<T, ?> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 0);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(SerializableFunction<T, ?> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 0);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(SerializablePredicate<T> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 0);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(InstanceIdFunction<T, ?> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 1);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(UniqueIdBiFunction<T, ?> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 1);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferInputType(SerializableConsumer<T> fn) {
    return (Class<T>) inferInputTypeFromAny(fn, 0);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> inferResultType(Object fn) {
    return (Class<T>) inferOutputType(inferMethodType(fn));
  }

  /**
   * Extracts the input type using the resolved interface signature. * @param fn The serializable
   * lambda
   *
   * @param lambdaParamIndex The index of the payload parameter in the interface's apply method
   */
  public static Class<?> inferInputTypeFromAny(Object fn, int lambdaParamIndex) {
    return inferInputType(inferMethodType(fn), lambdaParamIndex);
  }

  public static Optional<SerializedLambda> getSerializedLambda(Object fn) {
    try {
      return Optional.of(serializedLambda(fn));
    } catch (ReflectiveOperationException ex) {
      logger.debug("Error resolving serialized lambda for {}", fn, ex);
      return Optional.empty();
    }
  }

  private static SerializedLambda serializedLambda(Object fn)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method m = fn.getClass().getDeclaredMethod("writeReplace");
    m.setAccessible(true);
    return (SerializedLambda) m.invoke(fn);
  }

  public static Class<?> outputType(SerializedLambda lambda) {
    return inferOutputType(inferMethodType(lambda));
  }

  public static Class<?> inputType(SerializedLambda lambda) {
    return inferInputType(inferMethodType(lambda), 0);
  }

  private static Class<?> inferInputType(MethodType type, int index) {
    return type.parameterType(index);
  }

  private static Class<?> inferOutputType(MethodType type) {
    return type.returnType();
  }

  private static MethodType inferMethodType(SerializedLambda sl) {
    // getInstantiatedMethodType() provides the exact generic signature resolved
    // by the compiler, completely bypassing captured variables and method kind switches!
    return MethodType.fromMethodDescriptorString(
        sl.getInstantiatedMethodType(), sl.getClass().getClassLoader());
  }

  private static MethodType inferMethodType(Object fn) {
    try {
      SerializedLambda sl = serializedLambda(fn);
      return inferMethodType(sl);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(
          "Cannot infer type from lambda. Pass Class<T> or use a method reference.", ex);
    }
  }
}
