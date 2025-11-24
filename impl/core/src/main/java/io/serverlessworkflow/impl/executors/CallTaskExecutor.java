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
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import java.util.concurrent.CompletableFuture;

public class CallTaskExecutor<T extends TaskBase> extends RegularTaskExecutor<T> {

  private final CallableTask callable;

  public static class CallTaskExecutorBuilder<T extends TaskBase>
      extends RegularTaskExecutorBuilder<T> {
    private final CallableTaskBuilder<T> callable;

    protected CallTaskExecutorBuilder(
        WorkflowMutablePosition position,
        T task,
        WorkflowDefinition definition,
        CallableTaskBuilder<T> callable) {
      super(position, task, definition);
      this.callable = callable;
      callable.init(task, definition, position);
    }

    @Override
    public CallTaskExecutor<T> buildInstance() {
      return new CallTaskExecutor<>(this);
    }
  }

  protected CallTaskExecutor(CallTaskExecutorBuilder<T> builder) {
    super(builder);
    this.callable = builder.callable.build();
  }

  @Override
  protected CompletableFuture<WorkflowModel> internalExecute(
      WorkflowContext workflow, TaskContext taskContext) {
    return callable.apply(workflow, taskContext, taskContext.input());
  }
}
