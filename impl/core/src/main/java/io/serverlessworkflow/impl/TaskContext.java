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
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.executors.TransitionInfo;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TaskContext {

  private final JsonNode rawInput;
  private final TaskBase task;
  private final WorkflowPosition position;
  private final Instant startedAt;
  private final String taskName;
  private final Map<String, Object> contextVariables;
  private final Optional<TaskContext> parentContext;

  private JsonNode input;
  private JsonNode output;
  private JsonNode rawOutput;
  private Instant completedAt;
  private TransitionInfo transition;

  public TaskContext(
      JsonNode input,
      WorkflowPosition position,
      Optional<TaskContext> parentContext,
      String taskName,
      TaskBase task) {
    this(input, parentContext, taskName, task, position, Instant.now(), input, input, input);
  }

  private TaskContext(
      JsonNode rawInput,
      Optional<TaskContext> parentContext,
      String taskName,
      TaskBase task,
      WorkflowPosition position,
      Instant startedAt,
      JsonNode input,
      JsonNode output,
      JsonNode rawOutput) {
    this.rawInput = rawInput;
    this.parentContext = parentContext;
    this.taskName = taskName;
    this.task = task;
    this.position = position;
    this.startedAt = startedAt;
    this.input = input;
    this.output = output;
    this.rawOutput = rawOutput;
    this.contextVariables =
        parentContext.map(p -> new HashMap<>(p.contextVariables)).orElseGet(HashMap::new);
  }

  public TaskContext copy() {
    return new TaskContext(
        rawInput, parentContext, taskName, task, position, startedAt, input, output, rawOutput);
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

  public TaskBase task() {
    return task;
  }

  public TaskContext rawOutput(JsonNode output) {
    this.rawOutput = output;
    this.output = output;
    return this;
  }

  public JsonNode rawOutput() {
    return rawOutput;
  }

  public TaskContext output(JsonNode output) {
    this.output = output;
    return this;
  }

  public JsonNode output() {
    return output;
  }

  public WorkflowPosition position() {
    return position;
  }

  public Map<String, Object> variables() {
    return contextVariables;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Optional<TaskContext> parent() {
    return parentContext;
  }

  public String taskName() {
    return taskName;
  }

  public TaskContext completedAt(Instant instant) {
    this.completedAt = instant;
    return this;
  }

  public Instant completedAt() {
    return completedAt;
  }

  public TransitionInfo transition() {
    return transition;
  }

  public TaskContext transition(TransitionInfo transition) {
    this.transition = transition;
    return this;
  }
}
