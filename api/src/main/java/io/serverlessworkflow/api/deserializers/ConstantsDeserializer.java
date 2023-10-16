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
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.utils.Utils;
import io.serverlessworkflow.api.workflow.Constants;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstantsDeserializer extends StdDeserializer<Constants> {

  private static final long serialVersionUID = 510l;
  private static Logger logger = LoggerFactory.getLogger(ConstantsDeserializer.class);

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public ConstantsDeserializer() {
    this(Constants.class);
  }

  public ConstantsDeserializer(Class<?> vc) {
    super(vc);
  }

  public ConstantsDeserializer(WorkflowPropertySource context) {
    this(Constants.class);
    this.context = context;
  }

  @Override
  public Constants deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    JsonNode node = jp.getCodec().readTree(jp);

    Constants constants = new Constants();
    JsonNode constantsDefinition = null;

    if (node.isObject()) {
      constantsDefinition = node;
    } else {
      String constantsFileDef = node.asText();
      constants.setRefValue(constantsFileDef);
      String constantsFileSrc = Utils.getResourceFileAsString(constantsFileDef);
      if (constantsFileSrc != null && constantsFileSrc.trim().length() > 0) {
        JsonNode constantsRefNode = Utils.getNode(constantsFileSrc);
        JsonNode refConstants = constantsRefNode.get("constants");
        if (refConstants != null) {
          constantsDefinition = refConstants;
        } else {
          logger.error(
              "Unable to find constants definitions in reference file: {}", constantsFileSrc);
        }

      } else {
        logger.error("Unable to load constants defs reference file: {}", constantsFileSrc);
      }
    }
    constants.setConstantsDef(constantsDefinition);
    return constants;
  }
}
