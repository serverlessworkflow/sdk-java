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

import static io.serverlessworkflow.impl.lifecycle.LifecycleEventsUtils.publishEvent;

import io.serverlessworkflow.impl.executors.TaskExecutorHelper;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WorkflowMutableInstance implements WorkflowInstance {

  protected final AtomicReference<WorkflowStatus> status;
  private final String id;
  private final WorkflowModel input;

  private WorkflowContext workflowContext;
  private Instant startedAt;
  private Instant completedAt;
  private volatile WorkflowModel output;
  private CompletableFuture<WorkflowModel> completableFuture;

  WorkflowMutableInstance(WorkflowDefinition definition, WorkflowModel input) {
    this.id = definition.application().idFactory().get();
    this.input = input;
    this.status = new AtomicReference<>(WorkflowStatus.PENDING);
    definition.inputSchemaValidator().ifPresent(v -> v.validate(input));
    this.workflowContext = new WorkflowContext(definition, this);
  }

  @Override
  public CompletableFuture<WorkflowModel> start() {
    this.startedAt = Instant.now();
    this.status.set(WorkflowStatus.RUNNING);
    publishEvent(
        workflowContext, l -> l.onWorkflowStarted(new WorkflowStartedEvent(workflowContext)));
    this.completableFuture =
        TaskExecutorHelper.processTaskList(
                workflowContext.definition().startTask(),
                workflowContext,
                Optional.empty(),
                workflowContext
                    .definition()
                    .inputFilter()
                    .map(f -> f.apply(workflowContext, null, input))
                    .orElse(input))
            .whenComplete(this::whenFailed)
            .thenApply(this::whenSuccess);
    return completableFuture;
  }

  private void whenFailed(WorkflowModel result, Throwable ex) {
    completedAt = Instant.now();
    if (ex != null) {
      status.compareAndSet(WorkflowStatus.RUNNING, WorkflowStatus.FAULTED);
      publishEvent(
          workflowContext, l -> l.onWorkflowFailed(new WorkflowFailedEvent(workflowContext, ex)));
    }
  }

  private WorkflowModel whenSuccess(WorkflowModel node) {
    output =
        workflowContext
            .definition()
            .outputFilter()
            .map(f -> f.apply(workflowContext, null, node))
            .orElse(node);
    workflowContext.definition().outputSchemaValidator().ifPresent(v -> v.validate(output));
    status.compareAndSet(WorkflowStatus.RUNNING, WorkflowStatus.COMPLETED);
    publishEvent(
        workflowContext, l -> l.onWorkflowCompleted(new WorkflowCompletedEvent(workflowContext)));
    return output;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public Instant startedAt() {
    return startedAt;
  }

  @Override
  public Instant completedAt() {
    return completedAt;
  }

  @Override
  public WorkflowModel input() {
    return input;
  }

  @Override
  public WorkflowStatus status() {
    return status.get();
  }

  @Override
  public WorkflowModel output() {
    return output;
  }

  @Override
  public <T> T outputAs(Class<T> clazz) {
    return output
        .as(clazz)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Output " + output + " cannot be converted to class " + clazz));
  }

  public void status(WorkflowStatus state) {
    this.status.set(state);
  }

  @Override
  public String toString() {
    return "WorkflowMutableInstance [status="
        + status
        + ", id="
        + id
        + ", startedAt="
        + startedAt
        + ", completedAt="
        + completedAt
        + "]";
  }
}
