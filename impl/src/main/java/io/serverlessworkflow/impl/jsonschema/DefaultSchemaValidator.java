/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.serverlessworkflow.impl.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultSchemaValidator implements SchemaValidator {

  private final JsonNode jsonNode;
  private final AtomicReference<JsonSchema> schemaObject = new AtomicReference<>();

  public DefaultSchemaValidator(JsonNode jsonNode) {
    this.jsonNode = jsonNode;
  }

  @Override
  public void validate(JsonNode node) {
    Set<ValidationMessage> report = getSchema().validate(node);
    if (!report.isEmpty()) {
      StringBuilder sb = new StringBuilder("There are JsonSchema validation errors:");
      report.forEach(m -> sb.append(System.lineSeparator()).append(m.getMessage()));
      throw new IllegalArgumentException(sb.toString());
    }
  }

  private JsonSchema getSchema() {
    JsonSchema result = schemaObject.get();
    if (result == null) {
      result = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(jsonNode);
      schemaObject.set(result);
    }
    return result;
  }
}
