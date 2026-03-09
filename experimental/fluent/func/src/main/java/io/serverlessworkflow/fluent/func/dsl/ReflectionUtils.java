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
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

// Don't make this class public the ReflectionUtils must be accessed only by the DSL classes.

/**
 * Specially used by {@link Function} parameters in the Java Function.
 *
 * @see <a href="https://www.baeldung.com/java-serialize-lambda">Serialize a Lambda in Java</a>
 */
final class ReflectionUtils {

  private ReflectionUtils() {}

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(JavaContextFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(JavaFilterFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializableFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializablePredicate<T> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(InstanceIdBiFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(UniqueIdBiFunction<T, ?> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  @SuppressWarnings("unchecked")
  static <T> Class<T> inferInputType(SerializableConsumer<T> fn) {
    return throwIllegalStateIfNull((Class<T>) inferInputTypeFromAny(fn));
  }

  private static Class<?> inferInputTypeFromAny(Object fn) {
    try {
      Method m = fn.getClass().getDeclaredMethod("writeReplace");
      m.setAccessible(true);
      SerializedLambda sl = (SerializedLambda) m.invoke(fn);

      // Owner class of the referenced implementation
      String ownerName = sl.getImplClass().replace('/', '.');
      ClassLoader cl = fn.getClass().getClassLoader();
      Class<?> owner = Class.forName(ownerName, false, cl);

      // Parse the impl method descriptor to get raw param types
      MethodType mt = MethodType.fromMethodDescriptorString(sl.getImplMethodSignature(), cl);
      Class<?>[] params = mt.parameterArray();
      int kind = sl.getImplMethodKind();

      switch (kind) {
        case MethodHandleInfo.REF_invokeStatic:
        case MethodHandleInfo.REF_newInvokeSpecial:
          // static method or constructor: T is the first parameter
          return params.length >= 1 ? params[0] : null;

        case MethodHandleInfo.REF_invokeVirtual:
        case MethodHandleInfo.REF_invokeInterface:
        case MethodHandleInfo.REF_invokeSpecial:
          // instance method ref like Foo::bar
          // For Function<T,R>, if bar has no params, T is the receiver type (owner).
          // If bar has one param, that pattern would usually map to BiFunction, not Function,
          // but keep a defensive branch anyway:
          return (params.length == 0) ? owner : params[0];

        default:
          return null;
      }
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
