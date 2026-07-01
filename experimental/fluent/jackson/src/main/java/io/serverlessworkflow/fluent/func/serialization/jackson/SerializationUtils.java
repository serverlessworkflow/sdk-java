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
package io.serverlessworkflow.fluent.func.serialization.jackson;

import io.serverlessworkflow.api.reflection.func.ReflectionUtils;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.lang.invoke.SerializedLambda;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationUtils {

  private static final Logger logger = LoggerFactory.getLogger(SerializationUtils.class);

  private SerializationUtils() {}

  public static Optional<Class<?>> getClassOrEmpty(Object obj) {

    if (obj instanceof Optional optional) {
      return optional;
    } else if (obj instanceof Class<?> clazz) {
      return Optional.of(clazz);
    } else if (obj instanceof String str) {
      try {
        return Optional.of(ReflectionUtils.loadClass(str));
      } catch (ClassNotFoundException e) {
        logger.error("Error loading class {}. Returning optinal empty", str, e);
      }
    }
    return Optional.empty();
  }

  public static Optional<SerializedLambda> getSerializedLambda(Object obj) {
    return obj instanceof Map map
        ? Optional.of(JsonUtils.convertValue(map, SerializedLambda.class))
        : Optional.empty();
  }

  public static Object getFunction(Object obj) {
    try {
      return ReflectionUtils.functionFromSerialized(
          getSerializedLambda(obj)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Expecting function object to be a Map, but it was " + obj)));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
