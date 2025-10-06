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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutableInstance;
import io.serverlessworkflow.impl.executors.TransitionInfo;
import java.util.concurrent.CompletableFuture;

public class WorkflowPersistenceInstance extends WorkflowMutableInstance {

  private final PersistenceWorkflowInfo info;

  public WorkflowPersistenceInstance(WorkflowDefinition definition, PersistenceWorkflowInfo info) {
    super(definition, info.id(), info.input());
    this.info = info;
  }

  @Override
  public CompletableFuture<WorkflowModel> start() {
    return startExecution(
        () -> {
          startedAt = info.startedAt();
        });
  }

  @Override
  public void restoreContext(WorkflowContext workflow, TaskContext context) {
    PersistenceTaskInfo taskInfo = info.tasks().get(context.position().jsonPointer());
    if (taskInfo != null) {
      context.output(taskInfo.model());
      context.completedAt(taskInfo.instant());
      context.transition(
          new TransitionInfo(
              taskInfo.nextPosition() == null
                  ? null
                  : workflow.definition().taskExecutor(taskInfo.nextPosition()),
              taskInfo.isEndNode()));
      workflow.context(taskInfo.context());
    }
  }
}
