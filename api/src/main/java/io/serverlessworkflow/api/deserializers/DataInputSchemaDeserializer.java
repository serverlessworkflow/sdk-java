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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.datainputschema.DataInputSchema;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.io.IOException;

public class DataInputSchemaDeserializer extends StdDeserializer<DataInputSchema> {

  private static final long serialVersionUID = 510l;

  public DataInputSchemaDeserializer() {
    this(DataInputSchema.class);
  }

  public DataInputSchemaDeserializer(Class<?> vc) {
    super(vc);
  }

  public DataInputSchemaDeserializer(WorkflowPropertySource context) {
    this(DataInputSchema.class);
  }

  @Override
  public DataInputSchema deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {

    JsonNode node = jp.getCodec().readTree(jp);

    DataInputSchema dataInputSchema = new DataInputSchema();

    if (!node.isObject()) {
      dataInputSchema.setSchema(node.asText());
      dataInputSchema.setFailOnValidationErrors(true); // default

      return dataInputSchema;
    } else {
      dataInputSchema.setSchema(node.get("schema").asText());
      dataInputSchema.setFailOnValidationErrors(node.get("failOnValidationErrors").asBoolean());

      return dataInputSchema;
    }
  }
}
