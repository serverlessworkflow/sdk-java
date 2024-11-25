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
package io.serverlessworkflow.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.impl.json.JsonUtils;

public class WorkflowContext {
  private final WorkflowDefinition definition;
  private final JsonNode input;
  private JsonNode context;

  WorkflowContext(WorkflowDefinition definition, JsonNode input) {
    this.definition = definition;
    this.input = input;
    this.context = JsonUtils.mapper().createObjectNode();
  }

  public JsonNode context() {
    return context;
  }

  public void context(JsonNode context) {
    this.context = context;
  }

  public JsonNode rawInput() {
    return input;
  }

  public WorkflowDefinition definition() {
    return definition;
  }
}
