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

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class RegularTaskExecutor<T extends TaskBase> extends AbstractTaskExecutor<T> {

  protected TransitionInfo transition;

  protected RegularTaskExecutor(RegularTaskExecutorBuilder<T> builder) {
    super(builder);
  }

  public abstract static class RegularTaskExecutorBuilder<T extends TaskBase>
      extends AbstractTaskExecutorBuilder<T, RegularTaskExecutor<T>> {

    private TransitionInfoBuilder transition;

    protected RegularTaskExecutorBuilder(
        WorkflowMutablePosition position,
        T task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
    }

    public void connect(Map<String, TaskExecutorBuilder<?>> connections) {
      this.transition = next(task.getThen(), connections);
    }

    @Override
    protected void buildTransition(RegularTaskExecutor<T> instance) {
      instance.transition = TransitionInfo.build(transition);
    }
  }

  @Override
  protected TransitionInfo getSkipTransition() {
    return transition;
  }

  protected CompletableFuture<TaskContext> execute(
      WorkflowContext workflow, TaskContext taskContext) {
    CompletableFuture<TaskContext> future =
        internalExecute(workflow, taskContext)
            .thenApply(node -> taskContext.rawOutput(node).transition(transition));
    return future;
  }

  protected abstract CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext task);
}
