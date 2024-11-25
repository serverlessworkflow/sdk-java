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
  private final WorkflowPosition position;
  private final WorkflowDefinition definition;
  private final JsonNode input;
  private JsonNode current;
  private JsonNode context;

  private WorkflowContext(
      WorkflowPosition position, WorkflowDefinition definition, JsonNode input) {
    this.position = position;
    this.definition = definition;
    this.input = input;
    this.current = input.deepCopy();
    this.context = JsonUtils.mapper().createObjectNode();
  }

  public static Builder builder(WorkflowDefinition definition, JsonNode input) {
    return new Builder(definition, input);
  }

  public static class Builder {
    private WorkflowPosition position = new DefaultWorkflowPosition();
    private WorkflowDefinition definition;
    private JsonNode input;

    private Builder(WorkflowDefinition definition, JsonNode input) {
      this.definition = definition;
      this.input = input;
    }

    public Builder position(WorkflowPosition position) {
      this.position = position;
      return this;
    }

    public WorkflowContext build() {
      return new WorkflowContext(position, definition, input);
    }
  }

  public WorkflowPosition position() {
    return position;
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

  public void current(JsonNode output) {
    this.current = output;
  }

  public JsonNode current() {
    return current;
  }

  public WorkflowDefinition definition() {
    return definition;
  }
}
