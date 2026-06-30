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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.serverlessworkflow.api.reflection.func.ReflectionUtils;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallJava.CallJavaFunction;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.util.Optional;

public class CallJavaFunctionSerializer extends JsonSerializer<CallJava.CallJavaFunction> {

  @Override
  public void serialize(CallJavaFunction value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("call", "Java");
    gen.writeObjectFieldStart("with");
    Optional<SerializedLambda> serializedLambda =
        ReflectionUtils.getSerializedLambda(value.function());
    if (serializedLambda.isPresent()) {
      gen.writeObjectField("function", serializedLambda.orElse(null));
    } else {
      gen.writeStringField("function", value.function().toString());
    }
    gen.writeEndObject();
    gen.writeEndObject();
  }
}
