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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionDefinitionTypeDeserializer extends StdDeserializer<FunctionDefinition.Type> {

  private static final long serialVersionUID = 510l;
  private static Logger logger = LoggerFactory.getLogger(FunctionDefinitionTypeDeserializer.class);

  private WorkflowPropertySource context;

  public FunctionDefinitionTypeDeserializer() {
    this(FunctionDefinition.Type.class);
  }

  public FunctionDefinitionTypeDeserializer(WorkflowPropertySource context) {
    this(FunctionDefinition.Type.class);
    this.context = context;
  }

  public FunctionDefinitionTypeDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public FunctionDefinition.Type deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {

    String value = jp.getText();
    if (context != null) {
      try {
        String result = context.getPropertySource().getProperty(value);

        if (result != null) {
          return FunctionDefinition.Type.fromValue(result);
        } else {
          return FunctionDefinition.Type.fromValue(jp.getText());
        }
      } catch (Exception e) {
        logger.info("Exception trying to evaluate property: {}", e.getMessage());
        return FunctionDefinition.Type.fromValue(jp.getText());
      }
    } else {
      return FunctionDefinition.Type.fromValue(jp.getText());
    }
  }
}
