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
package io.serverlessworkflow.impl.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Set;

public class DefaultSchemaValidator implements SchemaValidator {

  private final JsonSchema schemaObject;

  public DefaultSchemaValidator(JsonNode jsonNode) {
    this.schemaObject = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(jsonNode);
  }

  @Override
  public void validate(WorkflowModel node) {
    Set<ValidationMessage> report =
        schemaObject.validate(
            node.as(JsonNode.class)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Default schema validator requires WorkflowModel to support conversion to json node")));
    if (!report.isEmpty()) {
      StringBuilder sb = new StringBuilder("There are JsonSchema validation errors:");
      report.forEach(m -> sb.append(System.lineSeparator()).append(m.getMessage()));
      throw new IllegalArgumentException(sb.toString());
    }
  }
}
