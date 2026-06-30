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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.serverlessworkflow.api.reflection.func.ReflectionUtils;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallJava.CallJavaFunction;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallJavaFunctionDeserializer extends JsonDeserializer<CallJava.CallJavaFunction> {

  private static Logger logger = LoggerFactory.getLogger(CallJavaFunctionDeserializer.class);

  @Override
  public CallJavaFunction deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {

    TreeNode content = p.readValueAsTree();
    TreeNode callNode = content.get("call");
    if (!(callNode instanceof TextNode textNode) || !textNode.asText().equals("Java")) {
      throw new JsonParseException(
          "Expecting a call property which value shoud be Java but instead it was " + callNode);
    }
    TreeNode functionNode = content.get("function");
    if (functionNode == null) {
      throw new JsonParseException("A CallJava function should have a function property");
    }
    if (functionNode instanceof ObjectNode objectNode) {
      SerializedLambda serializedLambda = ctxt.readTreeAsValue(objectNode, SerializedLambda.class);
      try {
        Method readResolve = SerializedLambda.class.getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);

        return new CallJavaFunction(
            (Function) readResolve.invoke(serializedLambda),
            Optional.of(ReflectionUtils.inputType(serializedLambda)),
            Optional.of(ReflectionUtils.outputType(serializedLambda)));
      } catch (ReflectiveOperationException e) {
        throw new IOException(
            "Reflection exception while converting SerializedLamda "
                + serializedLambda
                + "into a funcion");
      }

    } else {
      logger.error(
          "function property: {} , does not contain enough information to be properly unmarshall, using an function that does nothing",
          functionNode);
      return new CallJavaFunction<>(v -> v, Optional.empty(), Optional.empty());
    }
  }
}
