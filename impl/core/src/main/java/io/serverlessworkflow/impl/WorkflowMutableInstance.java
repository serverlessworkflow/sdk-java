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
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WorkflowMutableInstance implements WorkflowInstance {

  protected final AtomicReference<WorkflowStatus> status;
  protected final String id;
  protected final WorkflowModel input;

  protected final WorkflowContext workflowContext;
  protected Instant startedAt;

  protected AtomicReference<CompletableFuture<WorkflowModel>> futureRef = new AtomicReference<>();
  protected Instant completedAt;

  private Lock statusLock = new ReentrantLock();
  private Map<CompletableFuture<TaskContext>, TaskContext> suspended;

  protected WorkflowMutableInstance(WorkflowDefinition definition, String id, WorkflowModel input) {
    this.id = id;
    this.input = input;
    this.status = new AtomicReference<>(WorkflowStatus.PENDING);
    this.workflowContext = new WorkflowContext(definition, this);
  }

  @Override
  public CompletableFuture<WorkflowModel> start() {
    return startExecution(
        () -> {
          startedAt = Instant.now();
          status.set(WorkflowStatus.RUNNING);
          publishEvent(
              workflowContext, l -> l.onWorkflowStarted(new WorkflowStartedEvent(workflowContext)));
        });
  }

  protected final CompletableFuture<WorkflowModel> startExecution(Runnable runnable) {
    CompletableFuture<WorkflowModel> future = futureRef.get();
    if (future != null) {
      return future;
    }
    runnable.run();
    future =
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
    futureRef.set(future);
    return future;
  }

  private void whenFailed(WorkflowModel result, Throwable ex) {
    completedAt = Instant.now();
    if (ex != null) {
      handleException(ex instanceof CompletionException ? ex = ex.getCause() : ex);
    }
  }

  private void handleException(Throwable ex) {
    if (!(ex instanceof CancellationException)) {
      status.set(WorkflowStatus.FAULTED);
      publishEvent(
          workflowContext, l -> l.onWorkflowFailed(new WorkflowFailedEvent(workflowContext, ex)));
    }
  }

  private WorkflowModel whenSuccess(WorkflowModel node) {
    WorkflowModel output =
        workflowContext
            .definition()
            .outputFilter()
            .map(f -> f.apply(workflowContext, null, node))
            .orElse(node);
    workflowContext.definition().outputSchemaValidator().ifPresent(v -> v.validate(output));
    status.set(WorkflowStatus.COMPLETED);
    publishEvent(
        workflowContext,
        l -> l.onWorkflowCompleted(new WorkflowCompletedEvent(workflowContext, output)));
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
    return futureRef.get().join();
  }

  @Override
  public <T> T outputAs(Class<T> clazz) {
    return output()
        .as(clazz)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Output " + output() + " cannot be converted to class " + clazz));
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

  @Override
  public boolean suspend() {
    try {
      statusLock.lock();
      if (TaskExecutorHelper.isActive(status.get()) && suspended == null) {
        suspended = new ConcurrentHashMap<>();
        status.set(WorkflowStatus.SUSPENDED);
        publishEvent(
            workflowContext,
            l -> l.onWorkflowSuspended(new WorkflowSuspendedEvent(workflowContext)));
        return true;
      } else {
        return false;
      }
    } finally {
      statusLock.unlock();
    }
  }

  @Override
  public boolean resume() {
    try {
      statusLock.lock();
      if (TaskExecutorHelper.isActive(status.get()) && suspended != null) {
        publishEvent(
            workflowContext, l -> l.onWorkflowResumed(new WorkflowResumedEvent(workflowContext)));
        suspended.forEach(
            (k, v) -> {
              k.complete(v);
            });
        suspended = null;
        return true;
      }
    } finally {
      statusLock.unlock();
    }
    return false;
  }

  public CompletableFuture<TaskContext> cancelCheck(TaskContext t) {
    try {
      statusLock.lock();
      if (status.get() == WorkflowStatus.CANCELLED) {
        CompletableFuture<TaskContext> cancelled = new CompletableFuture<TaskContext>();
        cancelled.completeExceptionally(
            new CancellationException("Task " + t.taskName() + " has been cancelled"));
        return cancelled;
      }
    } finally {
      statusLock.unlock();
    }
    return CompletableFuture.completedFuture(t);
  }

  public CompletableFuture<TaskContext> suspendedCheck(TaskContext t) {
    try {
      statusLock.lock();
      if (suspended != null) {
        CompletableFuture<TaskContext> suspendedTask = new CompletableFuture<TaskContext>();
        suspended.put(suspendedTask, t);
        return suspendedTask;
      } else if (TaskExecutorHelper.isActive(status.get())) {
        status.set(WorkflowStatus.RUNNING);
      }
    } finally {
      statusLock.unlock();
    }
    return CompletableFuture.completedFuture(t);
  }

  @Override
  public boolean cancel() {
    try {
      statusLock.lock();
      if (TaskExecutorHelper.isActive(status.get())) {
        status.set(WorkflowStatus.CANCELLED);
        publishEvent(
            workflowContext,
            l -> l.onWorkflowCancelled(new WorkflowCancelledEvent(workflowContext)));
        return true;
      } else {
        return false;
      }
    } finally {
      statusLock.unlock();
    }
  }

  public void restoreContext(WorkflowContext workflow, TaskContext context) {}
}
