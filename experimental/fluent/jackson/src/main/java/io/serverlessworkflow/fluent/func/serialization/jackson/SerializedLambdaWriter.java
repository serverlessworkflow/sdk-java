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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import io.serverlessworkflow.api.reflection.func.ReflectionUtils;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;

public class SerializedLambdaWriter extends BeanPropertyWriter {

  private static final long serialVersionUID = 1L;

  public SerializedLambdaWriter(BeanPropertyWriter base) {
    super(base);
  }

  @Override
  public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
      throws IOException {
    SerializedLambda sl = (SerializedLambda) bean;
    int size = sl.getCapturedArgCount();
    if (size > 0) {
      gen.writeArrayFieldStart(SerializedLambdaDeserializer.CAPTURED_ARGS);
      for (int i = 0; i < size; i++) {
        Object obj = sl.getCapturedArg(i);
        gen.writeStartObject();
        if (obj != null) {
          gen.writeStringField(
              SerializedLambdaDeserializer.TYPE_CAPTURE_ARG,
              ReflectionUtils.serializedFromFuntion(obj)
                  .map(v -> SerializedLambda.class.getName())
                  .orElse(obj.getClass().getName()));
          gen.writeObjectField(SerializedLambdaDeserializer.DATA_CAPTURE_ARG, obj);
        }
        gen.writeEndObject();
      }
      gen.writeEndArray();
    }
  }
}
