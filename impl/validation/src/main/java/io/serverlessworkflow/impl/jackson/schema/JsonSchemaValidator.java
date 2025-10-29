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
package io.serverlessworkflow.impl.jackson.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.schema.SchemaValidator;
import java.util.Collection;

public class JsonSchemaValidator implements SchemaValidator {

  private final Schema schemaObject;

  public JsonSchemaValidator(JsonNode jsonNode) {
    this.schemaObject =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7).getSchema(jsonNode);
  }

  @Override
  public void validate(WorkflowModel node) {
    Collection<Error> report =
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
