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

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;

class GrpcChannelResolver {

  static final String GRPC_CHANNEL_PROVIDER = "grpcChannelProvider";

  static Channel channel(
      WorkflowContext workflowContext,
      TaskContext taskContext,
      GrpcRequestContext grpcRequestContext) {
    return workflowContext
        .definition()
        .application()
        .<Channel>additionalObject(GRPC_CHANNEL_PROVIDER, workflowContext, taskContext)
        .orElseGet(
            () ->
                ManagedChannelBuilder.forAddress(
                        grpcRequestContext.address(), grpcRequestContext.port())
                    .usePlaintext()
                    .build());
  }
}
