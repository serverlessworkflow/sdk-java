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
package io.serverlessworkflow.impl.persistence;

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.util.function.Consumer;

public abstract class AbstractPersistenceInstanceWriter<K> implements PersistenceInstanceWriter {

  private final PersistenceInstanceStore<K> store;

  protected AbstractPersistenceInstanceWriter(PersistenceInstanceStore<K> store) {
    this.store = store;
  }

  @Override
  public void started(WorkflowContextData workflowContext) {
    doTransaction(t -> t.writeInstanceData(key(workflowContext), workflowContext));
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

  protected void removeProcessInstance(WorkflowContextData workflowContext) {
    doTransaction(
        t -> {
          K key = key(workflowContext);
          t.removeInstanceData(key, workflowContext);
          t.removeStatus(key, workflowContext);
          t.removeTasks(key);
        });
  }

  @Override
  public void taskStarted(WorkflowContextData workflowContext, TaskContextData taskContext) {
    // not recording
  }

  @Override
  public void taskRetried(WorkflowContextData workflowContext, TaskContextData taskContext) {
    doTransaction(t -> t.writeRetryTask(key(workflowContext), workflowContext, taskContext));
  }

  @Override
  public void taskCompleted(WorkflowContextData workflowContext, TaskContextData taskContext) {
    doTransaction(t -> t.writeCompletedTask(key(workflowContext), workflowContext, taskContext));
  }

  @Override
  public void suspended(WorkflowContextData workflowContext) {
    doTransaction(
        t -> t.writeStatus(key(workflowContext), WorkflowStatus.SUSPENDED, workflowContext));
  }

  @Override
  public void resumed(WorkflowContextData workflowContext) {
    doTransaction(t -> t.removeStatus(key(workflowContext), workflowContext));
  }

  private void doTransaction(Consumer<PersistenceInstanceTransaction<K>> operations) {
    PersistenceInstanceTransaction<K> transaction = store.begin();
    try {
      operations.accept(transaction);
      transaction.commit();
    } catch (Exception ex) {
      transaction.rollback();
      throw ex;
    }
  }

  protected abstract K key(WorkflowContextData workflowContext);
}
