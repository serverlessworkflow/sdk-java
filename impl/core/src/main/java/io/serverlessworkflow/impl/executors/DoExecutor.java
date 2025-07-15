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
package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DoExecutor extends RegularTaskExecutor<DoTask> {

  private final TaskExecutor<?> taskExecutor;

  public static class DoExecutorBuilder extends RegularTaskExecutorBuilder<DoTask> {
    private TaskExecutor<?> taskExecutor;

    protected DoExecutorBuilder(
        WorkflowPosition position,
        DoTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      taskExecutor =
          TaskExecutorHelper.createExecutorList(
              position, task.getDo(), workflow, application, resourceLoader);
    }

    @Override
    public TaskExecutor<DoTask> buildInstance() {
      return new DoExecutor(this);
    }
  }

  private DoExecutor(DoExecutorBuilder builder) {
    super(builder);
    this.taskExecutor = builder.taskExecutor;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return TaskExecutorHelper.processTaskList(
        taskExecutor, workflow, Optional.of(taskContext), taskContext.input());
  }
}
