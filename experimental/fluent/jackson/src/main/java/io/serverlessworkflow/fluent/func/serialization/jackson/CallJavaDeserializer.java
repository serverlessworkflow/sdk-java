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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.func.CallJava;
import java.io.IOException;
import java.util.Map;

public class CallJavaDeserializer<T extends CallFunction> extends JsonDeserializer<T>
    implements ResolvableDeserializer {

  private JsonDeserializer<?> defaultDeserializer;

  public CallJavaDeserializer(JsonDeserializer<?> defaultDeserializer) {
    this.defaultDeserializer = defaultDeserializer;
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    CallFunction original = (CallFunction) defaultDeserializer.deserialize(p, ctxt);
    if (original.getCall().equals(CallJava.JAVA_CALL_KEY)) {
      FunctionArguments args = original.getWith();
      if (args != null) {
        Map<String, Object> props = args.getAdditionalProperties();
        try {
          return (T)
              CallJava.fromFunctionProperties(
                  props,
                  SerializationUtils.getSerializedLambda(props.get(CallJava.FUNCTION_NAME_KEY))
                      .orElseThrow(
                          () ->
                              new IOException(
                                  "Expecting function object to be a Map. Please check if you are using serializable lambdas in your workflow definition")));
        } catch (ReflectiveOperationException e) {
          throw new IOException("Error unmarshalling java call with args " + args, e);
        }
      }
    }
    return (T) original;
  }

  @Override
  public void resolve(DeserializationContext ctxt) throws JsonMappingException {
    ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
  }
}
