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

import io.serverlessworkflow.impl.executors.TaskExecutorHelper;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WorkflowInstance {
  private final AtomicReference<WorkflowStatus> status;
  private final String id;
  private final WorkflowModel input;

  private WorkflowContext workflowContext;
  private WorkflowDefinition definition;
  private Instant startedAt;
  private Instant completedAt;
  private volatile WorkflowModel output;
  private CompletableFuture<WorkflowModel> completableFuture;

  WorkflowInstance(WorkflowDefinition definition, WorkflowModel input) {
    this.id = definition.application().idFactory().get();
    this.input = input;
    this.definition = definition;
    this.status = new AtomicReference<>(WorkflowStatus.PENDING);
    definition.inputSchemaValidator().ifPresent(v -> v.validate(input));
  }

  public CompletableFuture<WorkflowModel> start() {
    this.startedAt = Instant.now();
    this.workflowContext = new WorkflowContext(definition, this);
    this.status.set(WorkflowStatus.RUNNING);
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
    return completableFuture;
  }

  private WorkflowModel whenCompleted(WorkflowModel node) {
    output =
        workflowContext
            .definition()
            .outputFilter()
            .map(f -> f.apply(workflowContext, null, node))
            .orElse(node);
    workflowContext.definition().outputSchemaValidator().ifPresent(v -> v.validate(output));
    status.compareAndSet(WorkflowStatus.RUNNING, WorkflowStatus.COMPLETED);
    completedAt = Instant.now();
    return output;
  }

  public String id() {
    return id;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Instant completedAt() {
    return completedAt;
  }

  public WorkflowModel input() {
    return input;
  }

  public WorkflowStatus status() {
    return status.get();
  }

  public void status(WorkflowStatus state) {
    this.status.set(state);
  }

  public WorkflowModel output() {
    return output;
  }

  public <T> T outputAs(Class<T> clazz) {
    return output
        .as(clazz)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Output " + output + " cannot be converted to class " + clazz));
  }
}
