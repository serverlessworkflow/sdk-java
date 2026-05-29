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
package io.serverlessworkflow.impl.lifecycle.ce;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.impl.lifecycle.TaskCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskResumedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskRetriedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskSuspendedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCancelledEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowResumedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowStatusEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowSuspendedEvent;

public interface WorkflowLifeCycleCloudEventFactory {

  CloudEvent build(CloudEventBuilder builder);

  TaskCompletedCEData build(TaskCompletedEvent ev);

  TaskFailedCEData build(TaskFailedEvent ev);

  TaskCancelledCEData build(TaskCancelledEvent ev);

  TaskResumedCEData build(TaskResumedEvent ev);

  TaskRetriedCEData build(TaskRetriedEvent ev);

  TaskStartedCEData build(TaskStartedEvent ev);

  TaskSuspendedCEData build(TaskSuspendedEvent ev);

  WorkflowCancelledCEData build(WorkflowCancelledEvent ev);

  WorkflowFailedCEData build(WorkflowFailedEvent ev);

  WorkflowResumedCEData build(WorkflowResumedEvent ev);

  WorkflowStartedCEData build(WorkflowStartedEvent ev);

  WorkflowStatusCEDataEvent build(WorkflowStatusEvent ev);

  WorkflowSuspendedCEData build(WorkflowSuspendedEvent ev);

  WorkflowCompletedCEData build(WorkflowCompletedEvent event);
}
