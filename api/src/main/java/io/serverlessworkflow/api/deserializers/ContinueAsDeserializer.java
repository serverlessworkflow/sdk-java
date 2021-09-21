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
import io.serverlessworkflow.api.end.ContinueAs;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.timeouts.WorkflowExecTimeout;
import java.io.IOException;

public class ContinueAsDeserializer extends StdDeserializer<ContinueAs> {

  private static final long serialVersionUID = 510l;

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public ContinueAsDeserializer() {
    this(ContinueAs.class);
  }

  public ContinueAsDeserializer(Class<?> vc) {
    super(vc);
  }

  public ContinueAsDeserializer(WorkflowPropertySource context) {
    this(ContinueAs.class);
    this.context = context;
  }

  @Override
  public ContinueAs deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    JsonNode node = jp.getCodec().readTree(jp);

    ContinueAs continueAs = new ContinueAs();

    if (!node.isObject()) {
      continueAs.setWorkflowId(node.asText());
      continueAs.setVersion(null);
      continueAs.setData(null);
      continueAs.setWorkflowExecTimeout(null);
      return continueAs;
    } else {
      if (node.get("workflowId") != null) {
        continueAs.setWorkflowId(node.get("workflowId").asText());
      }

      if (node.get("version") != null) {
        continueAs.setVersion(node.get("version").asText());
      }

      if (node.get("data") != null) {
        continueAs.setData(node.get("data").asText());
      }

      if (node.get("workflowExecTimeout") != null) {
        continueAs.setWorkflowExecTimeout(
            mapper.treeToValue(node.get("workflowExecTimeout"), WorkflowExecTimeout.class));
      }

      return continueAs;
    }
  }
}
