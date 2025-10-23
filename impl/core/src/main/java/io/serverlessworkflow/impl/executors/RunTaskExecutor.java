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

import io.serverlessworkflow.api.types.RunTask;
import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.CompletableFuture;

public class RunTaskExecutor<T extends RunTaskConfiguration> extends RegularTaskExecutor<RunTask> {

  private final RunnableTask<T> runnable;

  private static final ServiceLoader<RunnableTask> runnables =
      ServiceLoader.load(RunnableTask.class);

  public static class RunTaskExecutorBuilder extends RegularTaskExecutorBuilder<RunTask> {
    private RunnableTask runnable;

    protected RunTaskExecutorBuilder(
        WorkflowMutablePosition position, RunTask task, WorkflowDefinition definition) {
      super(position, task, definition);
      RunTaskConfiguration config = task.getRun().get();
      this.runnable =
          runnables.stream()
              .map(Provider::get)
              .filter(r -> r.accept(config.getClass()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new UnsupportedOperationException(
                          "No runnable found for operation " + config.getClass()));
      runnable.init(config, definition);
    }

    @Override
    public RunTaskExecutor buildInstance() {
      return new RunTaskExecutor(this);
    }
  }

  protected RunTaskExecutor(RunTaskExecutorBuilder builder) {
    super(builder);
    this.runnable = builder.runnable;
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return runnable.apply(workflow, taskContext, taskContext.input());
  }
}
