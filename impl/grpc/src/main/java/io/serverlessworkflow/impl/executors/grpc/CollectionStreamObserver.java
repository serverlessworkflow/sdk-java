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
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import java.util.concurrent.CompletableFuture;

class CollectionStreamObserver implements StreamObserver<Message> {
  private final WorkflowModelCollection modelCollection;
  private final WorkflowModelFactory modelFactory;
  private final CompletableFuture<WorkflowModel> future;

  public CollectionStreamObserver(WorkflowModelFactory modelFactory) {
    this.modelCollection = modelFactory.createCollection();
    this.modelFactory = modelFactory;
    this.future = new CompletableFuture<>();
  }

  @Override
  public void onNext(Message value) {
    modelCollection.add(ProtobufMessageUtils.convert(value, modelFactory));
  }

  @Override
  public void onError(Throwable t) {
    future.completeExceptionally(t);
  }

  @Override
  public void onCompleted() {
    future.complete(modelCollection);
  }

  public CompletableFuture<WorkflowModel> future() {
    return future;
  }
}
