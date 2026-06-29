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

import io.serverlessworkflow.api.types.CallGRPC;
import io.serverlessworkflow.api.types.GRPCArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.WithGRPCService;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;
import io.serverlessworkflow.impl.executors.CallableTaskFactory;
import java.util.Map;

public class GrpcExecutorBuilder implements CallableTaskBuilder<CallGRPC> {

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallGRPC.class);
  }

  @Override
  public CallableTaskFactory init(
      CallGRPC task, WorkflowDefinition definition, WorkflowMutablePosition position) {

    GRPCArguments with = task.getWith();
    WithGRPCService service = with.getService();

    WorkflowValueResolver<Map<String, Object>> arguments =
        WorkflowUtils.buildMapResolver(
            definition.application(),
            with.getArguments() != null ? with.getArguments().getAdditionalProperties() : Map.of());

    GrpcRequestContext grpcRequestContext =
        new GrpcRequestContext(
            service.getHost(), service.getPort(), with.getMethod(), service.getName());

    return () ->
        new GrpcExecutor(
            grpcRequestContext,
            arguments,
            definition
                .resourceLoader()
                .loadStatic(with.getProto().getEndpoint(), FileDescriptorReader::readDescriptor));
  }
}
