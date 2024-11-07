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

public class TaskContext<T extends TaskBase> {

  private final JsonNode rawInput;
  private final T task;

  private JsonNode input;
  private JsonNode output;
  private JsonNode rawOutput;

  public TaskContext(JsonNode rawInput, T task) {
    this.rawInput = rawInput;
    this.input = rawInput;
    this.task = task;
  }

  public void input(JsonNode input) {
    this.input = input;
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

  public void output(JsonNode output) {
    this.output = output;
  }

  public JsonNode output() {
    return output;
  }

  public JsonNode rawOutput() {
    return rawOutput;
  }
}
