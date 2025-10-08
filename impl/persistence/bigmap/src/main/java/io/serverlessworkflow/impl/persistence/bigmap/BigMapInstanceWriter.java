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
package io.serverlessworkflow.impl.persistence.bigmap;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceWriter;
import java.util.function.Consumer;

public abstract class BigMapInstanceWriter<K, V, T, S> implements PersistenceInstanceWriter {

  private BigMapInstanceStore<K, V, T, S> store;

  protected BigMapInstanceWriter(BigMapInstanceStore<K, V, T, S> store) {
    this.store = store;
  }

  private void doTransaction(Consumer<BigMapInstanceTransaction<K, V, T, S>> operations) {
    BigMapInstanceTransaction<K, V, T, S> transaction = store.begin();
    try {
      operations.accept(transaction);
      transaction.commit();
    } catch (Exception ex) {
      transaction.rollback();
      throw ex;
    }
  }

  @Override
  public void started(WorkflowContextData workflowContext) {
    doTransaction(
        t ->
            t.instanceData(workflowContext.definition())
                .put(key(workflowContext), marshallInstance(workflowContext.instanceData())));
  }

  @Override
  public void completed(WorkflowContextData workflowContext) {
    removeProcessInstance(workflowContext);
  }

  @Override
  public void failed(WorkflowContextData workflowContext, Throwable ex) {
    removeProcessInstance(workflowContext);
  }

  @Override
  public void aborted(WorkflowContextData workflowContext) {
    removeProcessInstance(workflowContext);
  }

  @Override
  public void taskStarted(WorkflowContextData workflowContext, TaskContextData taskContext) {}

  @Override
  public void taskCompleted(WorkflowContextData workflowContext, TaskContextData taskContext) {
    doTransaction(
        t ->
            t.tasks(key(workflowContext))
                .put(
                    taskContext.position().jsonPointer(),
                    marshallTaskCompleted(workflowContext, (TaskContext) taskContext)));
  }

  @Override
  public void suspended(WorkflowContextData workflowContext) {
    doTransaction(
        t ->
            t.status(workflowContext.definition())
                .put(key(workflowContext), marshallStatus(WorkflowStatus.SUSPENDED)));
  }

  @Override
  public void resumed(WorkflowContextData workflowContext) {
    doTransaction(t -> t.status(workflowContext.definition()).remove(key(workflowContext)));
  }

  protected void removeProcessInstance(WorkflowContextData workflowContext) {
    doTransaction(
        t -> {
          K key = key(workflowContext);
          t.instanceData(workflowContext.definition()).remove(key);
          t.status(workflowContext.definition()).remove(key);
          t.cleanupTasks(key);
        });
  }

  protected abstract K key(WorkflowContextData workflowContext);

  protected abstract V marshallInstance(WorkflowInstanceData instance);

  protected abstract T marshallTaskCompleted(
      WorkflowContextData workflowContext, TaskContext taskContext);

  protected abstract S marshallStatus(WorkflowStatus status);
}
