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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TaskContext<T extends TaskBase> {

  private final JsonNode rawInput;
  private final T task;
  private final WorkflowPosition position;
  private final Instant startedAt;

  private JsonNode input;
  private JsonNode output;
  private JsonNode rawOutput;
  private FlowDirective flowDirective;
  private Map<String, Object> contextVariables;
  private Instant completedAt;

  public TaskContext(JsonNode input, WorkflowPosition position) {
    this(input, null, position, Instant.now(), input, input, input, null, new HashMap<>());
  }

  public TaskContext(JsonNode input, TaskContext<?> taskContext, T task) {
    this(
        input,
        task,
        taskContext.position,
        Instant.now(),
        input,
        input,
        input,
        task.getThen(),
        new HashMap<>(taskContext.variables()));
  }

  private TaskContext(
      JsonNode rawInput,
      T task,
      WorkflowPosition position,
      Instant startedAt,
      JsonNode input,
      JsonNode output,
      JsonNode rawOutput,
      FlowDirective flowDirective,
      Map<String, Object> contextVariables) {
    this.rawInput = rawInput;
    this.task = task;
    this.position = position;
    this.startedAt = startedAt;
    this.input = input;
    this.output = output;
    this.rawOutput = rawOutput;
    this.flowDirective = flowDirective;
    this.contextVariables = contextVariables;
  }

  public TaskContext<T> copy() {
    return new TaskContext<T>(
        rawInput,
        task,
        position.copy(),
        startedAt,
        input,
        output,
        rawOutput,
        flowDirective,
        new HashMap<>(contextVariables));
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

  public Instant startedAt() {
    return startedAt;
  }

  public void completedAt(Instant instant) {
    this.completedAt = instant;
  }

  public Instant completedAt() {
    return completedAt;
  }
}
