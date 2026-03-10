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
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Specially used by {@link Function} parameters in the Java Function.
 *
 * @see <a href="https://www.baeldung.com/java-serialize-lambda">Serialize a Lambda in Java</a>
 */
final class ReflectionUtils {

  private ReflectionUtils() {}

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(JavaContextFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 0));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(JavaFilterFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 0));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializableFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 0));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializablePredicate<T> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 0));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(InstanceIdFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 1));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(UniqueIdBiFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 1));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializableConsumer<T> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn, 0));
  }

  /**
   * Extracts the input type using the resolved interface signature. * @param fn The serializable
   * lambda
   *
   * @param lambdaParamIndex The index of the payload parameter in the interface's apply method
   */
  private static Class<?> inferInputTypeFromAny(Object fn, int lambdaParamIndex) {
    try {
      Method m = fn.getClass().getDeclaredMethod("writeReplace");
      m.setAccessible(true);
      SerializedLambda sl = (SerializedLambda) m.invoke(fn);

      ClassLoader cl = fn.getClass().getClassLoader();

      // getInstantiatedMethodType() provides the exact generic signature resolved
      // by the compiler, completely bypassing captured variables and method kind switches!
      MethodType mt = MethodType.fromMethodDescriptorString(sl.getInstantiatedMethodType(), cl);

      return mt.parameterArray()[lambdaParamIndex];

    } catch (Exception ignore) {
      return null;
    }
  }

  private static <T> Class<T> throwIllegalStateIfNull(Class<T> clazz) {
    if (clazz == null) {
      throw new IllegalStateException(
          "Cannot infer input type from lambda. Pass Class<T> or use a method reference.");
    }
    return clazz;
  }
}
