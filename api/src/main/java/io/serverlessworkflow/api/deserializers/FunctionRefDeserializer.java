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
package io.serverlessworkflow.api.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.functions.FunctionRef;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.io.IOException;

public class FunctionRefDeserializer extends StdDeserializer<FunctionRef> {

  private static final long serialVersionUID = 510l;

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public FunctionRefDeserializer() {
    this(FunctionRef.class);
  }

  public FunctionRefDeserializer(Class<?> vc) {
    super(vc);
  }

  public FunctionRefDeserializer(WorkflowPropertySource context) {
    this(FunctionRef.class);
    this.context = context;
  }

  @Override
  public FunctionRef deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    JsonNode node = jp.getCodec().readTree(jp);

    FunctionRef functionRef = new FunctionRef();

    if (!node.isObject()) {
      functionRef.setRefName(node.asText());
      functionRef.setArguments(null);
      return functionRef;
    } else {
      if (node.get("arguments") != null) {
        functionRef.setArguments(mapper.treeToValue(node.get("arguments"), JsonNode.class));
      }

      if (node.get("refName") != null) {
        functionRef.setRefName(node.get("refName").asText());
      }

      if (node.get("selectionSet") != null) {
        functionRef.setSelectionSet(node.get("selectionSet").asText());
      }

      return functionRef;
    }
  }
}
