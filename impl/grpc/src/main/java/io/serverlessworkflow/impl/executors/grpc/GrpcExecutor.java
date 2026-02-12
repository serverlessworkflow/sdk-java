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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GrpcExecutor implements CallableTask {

  private final GrpcRequestContext requestContext;
  private final WorkflowValueResolver<Map<String, Object>> arguments;
  private final FileDescriptorContext fileDescriptorContext;
  private final ExternalResource proto;

  public GrpcExecutor(
      GrpcRequestContext builder,
      WorkflowValueResolver<Map<String, Object>> arguments,
      FileDescriptorContext fileDescriptorContext,
      ExternalResource proto) {
    this.requestContext = builder;
    this.arguments = arguments;
    this.fileDescriptorContext = fileDescriptorContext;
    this.proto = proto;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    return buildGrpcCallExecutor(
        workflowContext, taskContext, this.arguments.apply(workflowContext, taskContext, input));
  }

  private CompletableFuture<WorkflowModel> buildGrpcCallExecutor(
      WorkflowContext workflowContext, TaskContext taskContext, Map<String, Object> arguments) {

    Channel channel = GrpcChannelResolver.channel(workflowContext, taskContext, requestContext);

    String protoName = fileDescriptorContext.inputProto();

    DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        fileDescriptorContext.fileDescriptorSet().getFileList().stream()
            .filter(
                file ->
                    file.getName()
                        .equals(this.proto.getName() != null ? this.proto.getName() : protoName))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Proto file not found in descriptor set"));

    try {
      Descriptors.FileDescriptor fileDescriptor =
          Descriptors.FileDescriptor.buildFrom(
              fileDescriptorProto, new Descriptors.FileDescriptor[] {});

      Descriptors.ServiceDescriptor serviceDescriptor =
          fileDescriptor.findServiceByName(requestContext.service());

      Objects.requireNonNull(serviceDescriptor, "Service not found: " + requestContext.service());

      Descriptors.MethodDescriptor methodDescriptor =
          serviceDescriptor.findMethodByName(requestContext.method());

      Objects.requireNonNull(methodDescriptor, "Method not found: " + requestContext.method());

      MethodDescriptor.MethodType methodType = ProtobufMessageUtils.getMethodType(methodDescriptor);

      ClientCall<Message, Message> call =
          buildClientCall(channel, methodType, serviceDescriptor, methodDescriptor);

      return switch (methodType) {
        case CLIENT_STREAMING ->
            CompletableFuture.completedFuture(
                handleClientStreaming(workflowContext, arguments, methodDescriptor, call));
        case BIDI_STREAMING ->
            CompletableFuture.completedFuture(
                handleBidiStreaming(workflowContext, arguments, methodDescriptor, call));
        case SERVER_STREAMING ->
            CompletableFuture.completedFuture(
                handleServerStreaming(workflowContext, methodDescriptor, arguments, call));
        case UNARY, UNKNOWN -> handleAsyncUnary(workflowContext, methodDescriptor, arguments, call);
      };

    } catch (Descriptors.DescriptorValidationException
        | InvalidProtocolBufferException
        | JsonProcessingException e) {
      throw new WorkflowException(WorkflowError.runtime(taskContext, e).build());
    }
  }

  private static ClientCall<Message, Message> buildClientCall(
      Channel channel,
      MethodDescriptor.MethodType methodType,
      Descriptors.ServiceDescriptor serviceDescriptor,
      Descriptors.MethodDescriptor methodDescriptor) {
    return channel.newCall(
        MethodDescriptor.<Message, Message>newBuilder()
            .setType(methodType)
            .setFullMethodName(
                MethodDescriptor.generateFullMethodName(
                    serviceDescriptor.getFullName(), methodDescriptor.getName()))
            .setRequestMarshaller(
                ProtoUtils.marshaller(
                    DynamicMessage.newBuilder(methodDescriptor.getInputType()).buildPartial()))
            .setResponseMarshaller(
                ProtoUtils.marshaller(
                    DynamicMessage.newBuilder(methodDescriptor.getOutputType()).buildPartial()))
            .build(),
        CallOptions.DEFAULT.withWaitForReady());
  }

  private static WorkflowModel handleClientStreaming(
      WorkflowContext workflowContext,
      Map<String, Object> parameters,
      Descriptors.MethodDescriptor methodDescriptor,
      ClientCall<Message, Message> call) {

    WorkflowModel workflowModel =
        ProtobufMessageUtils.asyncStreamingCall(
            parameters,
            methodDescriptor,
            responseObserver -> ClientCalls.asyncClientStreamingCall(call, responseObserver),
            workflowContext.definition().application().modelFactory());

    return workflowModel.asCollection().isEmpty()
        ? workflowContext.definition().application().modelFactory().fromNull()
        : (WorkflowModelCollection) workflowModel.asCollection();
  }

  private static WorkflowModel handleBidiStreaming(
      WorkflowContext workflowContext,
      Map<String, Object> parameters,
      Descriptors.MethodDescriptor methodDescriptor,
      ClientCall<Message, Message> call) {

    return ProtobufMessageUtils.asyncStreamingCall(
        parameters,
        methodDescriptor,
        responseObserver -> ClientCalls.asyncBidiStreamingCall(call, responseObserver),
        workflowContext.definition().application().modelFactory());
  }

  private static WorkflowModel handleServerStreaming(
      WorkflowContext workflowContext,
      Descriptors.MethodDescriptor methodDescriptor,
      Map<String, Object> parameters,
      ClientCall<Message, Message> call)
      throws InvalidProtocolBufferException, JsonProcessingException {
    Message.Builder builder = ProtobufMessageUtils.buildMessage(methodDescriptor, parameters);
    WorkflowModelCollection modelCollection =
        workflowContext.definition().application().modelFactory().createCollection();
    ClientCalls.blockingServerStreamingCall(call, builder.build())
        .forEachRemaining(
            message ->
                modelCollection.add(
                    ProtobufMessageUtils.convert(
                        message, workflowContext.definition().application().modelFactory())));
    return modelCollection;
  }

  private static CompletableFuture<WorkflowModel> handleAsyncUnary(
      WorkflowContext workflowContext,
      Descriptors.MethodDescriptor methodDescriptor,
      Map<String, Object> parameters,
      ClientCall<Message, Message> call)
      throws InvalidProtocolBufferException, JsonProcessingException {

    CompletableFuture<WorkflowModel> future = new CompletableFuture<>();

    Message.Builder builder = ProtobufMessageUtils.buildMessage(methodDescriptor, parameters);

    ClientCalls.asyncUnaryCall(
        call,
        builder.build(),
        new StreamObserver<>() {
          @Override
          public void onNext(Message value) {
            WorkflowModel model =
                ProtobufMessageUtils.convert(
                    value, workflowContext.definition().application().modelFactory());
            future.complete(model);
          }

          @Override
          public void onError(Throwable t) {
            future.completeExceptionally(t);
          }

          @Override
          public void onCompleted() {
            // no-op
          }
        });
    return future;
  }
}
