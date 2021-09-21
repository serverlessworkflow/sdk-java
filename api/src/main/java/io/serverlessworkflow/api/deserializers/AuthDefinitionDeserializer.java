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
import io.serverlessworkflow.api.auth.AuthDefinition;
import io.serverlessworkflow.api.auth.BasicAuthDefinition;
import io.serverlessworkflow.api.auth.BearerAuthDefinition;
import io.serverlessworkflow.api.auth.OauthDefinition;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.io.IOException;

public class AuthDefinitionDeserializer extends StdDeserializer<AuthDefinition> {

  private static final long serialVersionUID = 510l;

  @SuppressWarnings("unused")
  private WorkflowPropertySource context;

  public AuthDefinitionDeserializer() {
    this(AuthDefinition.class);
  }

  public AuthDefinitionDeserializer(Class<?> vc) {
    super(vc);
  }

  public AuthDefinitionDeserializer(WorkflowPropertySource context) {
    this(AuthDefinition.class);
    this.context = context;
  }

  @Override
  public AuthDefinition deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    JsonNode node = jp.getCodec().readTree(jp);

    AuthDefinition authDefinition = new AuthDefinition();

    if (node.get("name") != null) {
      authDefinition.setName(node.get("name").asText());
    }

    if (node.get("scheme") != null) {
      authDefinition.setScheme(AuthDefinition.Scheme.fromValue(node.get("scheme").asText()));
    }

    if (node.get("properties") != null) {
      JsonNode propsNode = node.get("properties");

      if (propsNode.get("grantType") != null) {
        authDefinition.setOauth(mapper.treeToValue(propsNode, OauthDefinition.class));
      } else if (propsNode.get("token") != null) {
        authDefinition.setBearerauth(mapper.treeToValue(propsNode, BearerAuthDefinition.class));
      } else {
        authDefinition.setBasicauth(mapper.treeToValue(propsNode, BasicAuthDefinition.class));
      }
    }

    return authDefinition;
  }
}
