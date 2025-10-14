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
package io.serverlessworkflow.impl.test;

import io.serverlessworkflow.impl.lifecycle.TaskCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskResumedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskRetriedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskSuspendedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceExecutionListener implements WorkflowExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(TraceExecutionListener.class);

  public void onWorkflowStarted(WorkflowStartedEvent ev) {
    logger.info(
        "Workflow definition {} with id {} started at {} with data {}",
        ev.workflowContext().definition().workflow().getDocument().getName(),
        ev.workflowContext().instanceData().id(),
        ev.eventDate(),
        ev.workflowContext().instanceData().input());
  }

  public void onWorkflowResumed(WorkflowResumedEvent ev) {
    logger.info(
        "Workflow definition {} with id {} resumed at {}",
        ev.workflowContext().definition().workflow().getDocument().getName(),
        ev.workflowContext().instanceData().id(),
        ev.eventDate());
  }

  public void onWorkflowSuspended(WorkflowSuspendedEvent ev) {
    logger.info(
        "Workflow definition {} with id {} suspended at {}",
        ev.workflowContext().definition().workflow().getDocument().getName(),
        ev.workflowContext().instanceData().id(),
        ev.eventDate());
  }

  public void onWorkflowCompleted(WorkflowCompletedEvent ev) {
    logger.info(
        "Workflow definition {} with id {} completed at {}",
        ev.workflowContext().definition().workflow().getDocument().getName(),
        ev.workflowContext().instanceData().id(),
        ev.eventDate());
  }

  public void onWorkflowFailed(WorkflowFailedEvent ev) {}

  public void onWorkflowCancelled(WorkflowCancelledEvent ev) {}

  public void onTaskStarted(TaskStartedEvent ev) {
    logger.info(
        "Task {} started at {}, position {}",
        ev.taskContext().taskName(),
        ev.eventDate(),
        ev.taskContext().position());
  }

  public void onTaskCompleted(TaskCompletedEvent ev) {
    logger.info(
        "Task {} completed at {} with output {}",
        ev.taskContext().taskName(),
        ev.eventDate(),
        ev.taskContext().output().asJavaObject());
  }

  public void onTaskFailed(TaskFailedEvent ev) {}

  public void onTaskCancelled(TaskCancelledEvent ev) {}

  public void onTaskSuspended(TaskSuspendedEvent ev) {}

  public void onTaskResumed(TaskResumedEvent ev) {}

  public void onTaskRetried(TaskRetriedEvent ev) {}
}
