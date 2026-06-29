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
package io.serverlessworkflow.impl.executors.grpc;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import java.util.concurrent.CompletableFuture;

abstract class ModelStreamObserver<T extends WorkflowModel> implements StreamObserver<Message> {
  protected T model;
  protected final WorkflowModelFactory modelFactory;
  private final CompletableFuture<WorkflowModel> future;
  private final TaskContext taskContext;

  public ModelStreamObserver(WorkflowContext workflowContext, TaskContext taskContext) {
    this.modelFactory = workflowContext.definition().application().modelFactory();
    this.taskContext = taskContext;
    this.future = new CompletableFuture<>();
  }

  @Override
  public void onError(Throwable t) {
    future.completeExceptionally(
        new WorkflowException(WorkflowError.runtime(taskContext, t).build(), t));
  }

  @Override
  public void onCompleted() {
    future.complete(model);
  }

  public CompletableFuture<WorkflowModel> future() {
    return future;
  }
}
