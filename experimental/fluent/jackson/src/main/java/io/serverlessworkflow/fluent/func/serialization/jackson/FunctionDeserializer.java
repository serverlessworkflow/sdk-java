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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.serverlessworkflow.api.types.utils.ReflectionUtils;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;

public class FunctionDeserializer<T> extends JsonDeserializer<T> {

  private final Class<T> objectClass;

  public FunctionDeserializer(Class<T> objectClass) {
    this.objectClass = objectClass;
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    try {
      return objectClass.cast(
          ReflectionUtils.functionFromSerialized(p.readValueAs(SerializedLambda.class)));
    } catch (ReflectiveOperationException e) {
      throw new IOException(e);
    }
  }
}
