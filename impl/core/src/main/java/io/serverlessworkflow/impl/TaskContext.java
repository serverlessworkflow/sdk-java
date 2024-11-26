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
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.TaskBase;
import java.util.HashMap;
import java.util.Map;

public class TaskContext<T extends TaskBase> {

  private final JsonNode rawInput;
  private final T task;
  private final WorkflowPosition position;

  private JsonNode input;
  private JsonNode output;
  private JsonNode rawOutput;
  private FlowDirective flowDirective;
  private Map<String, Object> contextVariables;

  public TaskContext(JsonNode input, WorkflowPosition position) {
    this.rawInput = input;
    this.position = position;
    this.task = null;
    this.contextVariables = new HashMap<>();
    init();
  }

  public TaskContext(JsonNode input, TaskContext<?> taskContext, T task) {
    this.rawInput = input;
    this.position = taskContext.position.copy();
    this.task = task;
    this.flowDirective = task.getThen();
    this.contextVariables = new HashMap<>(taskContext.variables());
    init();
  }

  private void init() {
    this.input = rawInput;
    this.rawOutput = rawInput;
    this.output = rawInput;
  }

  public void input(JsonNode input) {
    this.input = input;
    this.rawOutput = input;
    this.output = input;
  }

  public JsonNode input() {
    return input;
  }

  public JsonNode rawInput() {
    return rawInput;
  }

  public T task() {
    return task;
  }

  public void rawOutput(JsonNode output) {
    this.rawOutput = output;
    this.output = output;
  }

  public JsonNode rawOutput() {
    return rawOutput;
  }

  public void output(JsonNode output) {
    this.output = output;
  }

  public JsonNode output() {
    return output;
  }

  public void flowDirective(FlowDirective flowDirective) {
    this.flowDirective = flowDirective;
  }

  public FlowDirective flowDirective() {
    return flowDirective == null
        ? new FlowDirective().withFlowDirectiveEnum(FlowDirectiveEnum.CONTINUE)
        : flowDirective;
  }

  public Map<String, Object> variables() {
    return contextVariables;
  }

  public WorkflowPosition position() {
    return position;
  }
}
