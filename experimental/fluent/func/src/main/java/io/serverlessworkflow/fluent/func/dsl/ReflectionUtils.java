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

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

/**
 * Specially used by {@link Function} parameters in the Java Function.
 *
 * @see <a href="https://www.baeldung.com/java-serialize-lambda">Serialize a Lambda in Java</a>
 */
public final class ReflectionUtils {

  private ReflectionUtils() {}

  @SuppressWarnings("unchecked")
  static <T> Optional<Class<T>> safeInferInputType(Function<T, ?> fn) {
    try {
      Method m = fn.getClass().getDeclaredMethod("writeReplace");
      m.setAccessible(true);
      java.lang.invoke.SerializedLambda sl = (java.lang.invoke.SerializedLambda) m.invoke(fn);

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
          return params.length >= 1 ? Optional.of((Class<T>) params[0]) : Optional.empty();

        case MethodHandleInfo.REF_invokeVirtual:
        case MethodHandleInfo.REF_invokeInterface:
        case MethodHandleInfo.REF_invokeSpecial:
          // instance method ref like Foo::bar
          // For Function<T,R>, if bar has no params, T is the receiver type (owner).
          // If bar has one param, that pattern would usually map to BiFunction, not Function,
          // but keep a defensive branch anyway:
          return (params.length == 0)
              ? Optional.of((Class<T>) owner)
              : Optional.of((Class<T>) params[0]);

        default:
          return Optional.empty();
      }
    } catch (Exception ignore) {
      return Optional.empty();
    }
  }

  public static <T> Class<T> inferInputType(Function<T, ?> fn) {
    return safeInferInputType(fn)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Cannot infer input type from lambda. Pass Class<T> or use a method reference."));
  }
}
