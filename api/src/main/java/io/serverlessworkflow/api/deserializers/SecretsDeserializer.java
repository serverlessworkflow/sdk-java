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
import io.serverlessworkflow.api.workflow.Secrets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretsDeserializer extends StdDeserializer<Secrets> {

  private static final long serialVersionUID = 510l;
  private static Logger logger = LoggerFactory.getLogger(SecretsDeserializer.class);

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public SecretsDeserializer() {
    this(Secrets.class);
  }

  public SecretsDeserializer(Class<?> vc) {
    super(vc);
  }

  public SecretsDeserializer(WorkflowPropertySource context) {
    this(Secrets.class);
    this.context = context;
  }

  @Override
  public Secrets deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    Secrets secrets = new Secrets();
    List<String> secretsDefinitions = new ArrayList<>();

    if (node.isArray()) {
      for (final JsonNode nodeEle : node) {
        secretsDefinitions.add(nodeEle.asText());
      }
    } else {
      String secretsFileDef = node.asText();
      String secretsFileSrc = Utils.getResourceFileAsString(secretsFileDef);
      JsonNode secretsRefNode;
      ObjectMapper jsonWriter = new ObjectMapper();
      if (secretsFileSrc != null && secretsFileSrc.trim().length() > 0) {
        // if its a yaml def convert to json first
        if (!secretsFileSrc.trim().startsWith("{")) {
          // convert yaml to json to validate
          ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
          Object obj = yamlReader.readValue(secretsFileSrc, Object.class);

          secretsRefNode =
              jsonWriter.readTree(new JSONObject(jsonWriter.writeValueAsString(obj)).toString());
        } else {
          secretsRefNode = jsonWriter.readTree(new JSONObject(secretsFileSrc).toString());
        }

        JsonNode refSecrets = secretsRefNode.get("secrets");
        if (refSecrets != null) {
          for (final JsonNode nodeEle : refSecrets) {
            secretsDefinitions.add(nodeEle.asText());
          }
        } else {
          logger.error("Unable to find secrets definitions in reference file: {}", secretsFileSrc);
        }

      } else {
        logger.error("Unable to load secrets defs reference file: {}", secretsFileSrc);
      }
    }
    secrets.setSecretDefs(secretsDefinitions);
    return secrets;
  }
}
