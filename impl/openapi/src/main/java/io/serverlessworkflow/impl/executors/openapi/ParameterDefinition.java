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
package io.serverlessworkflow.impl.executors.openapi;

import com.fasterxml.jackson.databind.JsonNode;

class ParameterDefinition {

  private final String name;
  private final String in;
  private final boolean required;
  private final UnifiedOpenAPI.Schema schema;

  ParameterDefinition(JsonNode parameter) {
    this(
        parameter.get("name").asText(),
        parameter.get("in").asText(),
        parameter.has("required") && parameter.get("required").asBoolean(),
        null);
  }

  ParameterDefinition(String name, String in, boolean required, UnifiedOpenAPI.Schema schema) {
    this.name = name;
    this.in = in;
    this.required = required;
    this.schema = schema;
  }

  public String getIn() {
    return in;
  }

  public String getName() {
    return name;
  }

  public boolean getRequired() {
    return required;
  }

  public UnifiedOpenAPI.Schema getSchema() {
    return schema;
  }
}
