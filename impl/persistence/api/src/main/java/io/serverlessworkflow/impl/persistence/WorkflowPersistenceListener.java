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

import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;

public class WorkflowPersistenceListener implements WorkflowExecutionListener {

  private final WorkflowPersistenceWriter persistenceStore;

  public WorkflowPersistenceListener(WorkflowPersistenceWriter persistenceStore) {
    this.persistenceStore = persistenceStore;
  }

  @Override
  public void onWorkflowStarted(WorkflowStartedEvent ev) {
    persistenceStore.started(ev.workflowContext());
  }

  @Override
  public void onWorkflowFailed(WorkflowFailedEvent ev) {
    persistenceStore.failed(ev.workflowContext(), ev.cause());
  }

  @Override
  public void onWorkflowCancelled(WorkflowCancelledEvent ev) {
    persistenceStore.aborted(ev.workflowContext());
  }

  @Override
  public void onWorkflowSuspended(WorkflowSuspendedEvent ev) {
    persistenceStore.suspended(ev.workflowContext());
  }

  @Override
  public void onWorkflowResumed(WorkflowResumedEvent ev) {
    persistenceStore.resumed(ev.workflowContext());
  }

  @Override
  public void onTaskStarted(TaskStartedEvent ev) {
    persistenceStore.updated(ev.workflowContext(), ev.taskContext());
  }
}
