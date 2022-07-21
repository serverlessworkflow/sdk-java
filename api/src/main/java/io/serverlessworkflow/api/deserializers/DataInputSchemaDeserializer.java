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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.utils.Utils;
import io.serverlessworkflow.api.workflow.DataInputSchema;
import java.io.IOException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataInputSchemaDeserializer extends StdDeserializer<DataInputSchema> {

  private static final long serialVersionUID = 510l;
  private static Logger logger = LoggerFactory.getLogger(DataInputSchemaDeserializer.class);

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
    JsonNode schemaDefinition = null;

    if (node.isObject() && node.get("schema").isObject() && !node.get("schema").isEmpty()) {
      schemaDefinition = node.get("schema");
      dataInputSchema.setFailOnValidationErrors(node.get("failOnValidationErrors").asBoolean());
    } else {
      String schemaFileDef = node.isObject() ? node.get("schema").asText() : node.asText();
      dataInputSchema.setFailOnValidationErrors(true);
      dataInputSchema.setRefValue(schemaFileDef);
      String schemaFileContent = Utils.getResourceFileAsString(schemaFileDef);
      JsonNode schemaRefNode;
      ObjectMapper jsonWriter = new ObjectMapper();
      if (schemaFileContent != null && schemaFileContent.trim().length() > 0) {
        // if its a yaml def convert to json first
        if (!schemaFileContent.trim().startsWith("{")) {
          // convert yaml to json to validate
          ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
          Object schemaObj = yamlReader.readValue(schemaFileContent, Object.class);
          schemaRefNode =
              jsonWriter.readTree(
                  new JSONObject(jsonWriter.writeValueAsString(schemaObj)).toString());
        } else {
          schemaRefNode = jsonWriter.readTree(new JSONObject(schemaFileContent).toString());
        }

        JsonNode schemaRef = schemaRefNode.get("schema");
        if (schemaRef != null) {
          schemaDefinition = schemaRef;
        } else {
          logger.error(
              "Unable to find schema definitions in reference file: {}", schemaFileContent);
        }

      } else {
        logger.error("Unable to load schema defs reference file: {}", schemaFileContent);
      }
    }
    dataInputSchema.setSchemaDef(schemaDefinition);
    return dataInputSchema;
  }
}
