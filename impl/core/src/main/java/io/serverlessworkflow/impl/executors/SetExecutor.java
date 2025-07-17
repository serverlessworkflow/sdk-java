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

import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.concurrent.CompletableFuture;

public class SetExecutor extends RegularTaskExecutor<SetTask> {

  private final WorkflowFilter setFilter;

  public static class SetExecutorBuilder extends RegularTaskExecutorBuilder<SetTask> {

    private final WorkflowFilter setFilter;

    protected SetExecutorBuilder(
        WorkflowPosition position,
        SetTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      Set setInfo = task.getSet();
      SetTaskConfiguration setConfig = setInfo.getSetTaskConfiguration();
      this.setFilter =
          WorkflowUtils.buildWorkflowFilter(
              application,
              setInfo.getString(),
              setConfig != null ? setConfig.getAdditionalProperties() : null);
    }

    @Override
    public TaskExecutor<SetTask> buildInstance() {
      return new SetExecutor(this);
    }
  }

  private SetExecutor(SetExecutorBuilder builder) {
    super(builder);
    this.setFilter = builder.setFilter;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return CompletableFuture.completedFuture(
        setFilter.apply(workflow, taskContext, taskContext.input()));
  }
}
