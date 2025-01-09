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
import io.serverlessworkflow.impl.executors.TaskExecutorHelper;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WorkflowInstance {
  private final AtomicReference<WorkflowStatus> status;
  private final String id;
  private final JsonNode input;

  private final Instant startedAt;
  private CompletableFuture<JsonNode> completableFuture;
  private final WorkflowContext workflowContext;

  WorkflowInstance(WorkflowDefinition definition, JsonNode input) {
    this.id = definition.idFactory().get();
    this.input = input;
    definition.inputSchemaValidator().ifPresent(v -> v.validate(input));
    this.startedAt = Instant.now();
    this.workflowContext = new WorkflowContext(definition, this);
    this.status = new AtomicReference<>(WorkflowStatus.RUNNING);
    this.completableFuture =
        TaskExecutorHelper.processTaskList(
                definition.startTask(),
                workflowContext,
                Optional.empty(),
                definition
                    .inputFilter()
                    .map(f -> f.apply(workflowContext, null, input))
                    .orElse(input))
            .thenApply(this::whenCompleted);
  }

  private JsonNode whenCompleted(JsonNode node) {
    JsonNode model =
        workflowContext
            .definition()
            .outputFilter()
            .map(f -> f.apply(workflowContext, null, node))
            .orElse(node);
    workflowContext.definition().outputSchemaValidator().ifPresent(v -> v.validate(model));
    status.compareAndSet(WorkflowStatus.RUNNING, WorkflowStatus.COMPLETED);
    return model;
  }

  public String id() {
    return id;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public JsonNode input() {
    return input;
  }

  public WorkflowStatus status() {
    return status.get();
  }

  public void status(WorkflowStatus state) {
    this.status.set(state);
  }

  public CompletableFuture<Object> output() {
    return outputAsJsonNode().thenApply(JsonUtils::toJavaValue);
  }

  public CompletableFuture<JsonNode> outputAsJsonNode() {
    return completableFuture.thenApply(this::whenCompleted);
  }
}
