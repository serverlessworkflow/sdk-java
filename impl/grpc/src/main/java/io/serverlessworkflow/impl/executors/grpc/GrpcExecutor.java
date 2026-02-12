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
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GrpcExecutor implements CallableTask {

  private final GrpcRequestContext requestContext;
  private final WorkflowValueResolver<Map<String, Object>> arguments;
  private final FileDescriptorContext fileDescriptorContext;

  public GrpcExecutor(
      GrpcRequestContext builder,
      WorkflowValueResolver<Map<String, Object>> arguments,
      FileDescriptorContext fileDescriptorContext) {
    this.requestContext = builder;
    this.arguments = arguments;
    this.fileDescriptorContext = fileDescriptorContext;
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

    DescriptorProtos.FileDescriptorProto fileDescriptorProto =
        fileDescriptorContext.fileDescriptorSet().getFileList().stream()
            .filter(file -> file.getName().equals(fileDescriptorContext.inputProto()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Proto file not found in descriptor set"));

    try {
      Descriptors.FileDescriptor fileDescriptor =
          Descriptors.FileDescriptor.buildFrom(
              fileDescriptorProto, new Descriptors.FileDescriptor[] {});

      Descriptors.ServiceDescriptor serviceDescriptor =
          Objects.requireNonNull(
              fileDescriptor.findServiceByName(requestContext.service()),
              "Service not found: " + requestContext.service());

      Descriptors.MethodDescriptor methodDescriptor =
          Objects.requireNonNull(
              serviceDescriptor.findMethodByName(requestContext.method()),
              "Method not found: " + requestContext.method());

      MethodDescriptor.MethodType methodType = ProtobufMessageUtils.getMethodType(methodDescriptor);

      ClientCall<Message, Message> call =
          buildClientCall(channel, methodType, serviceDescriptor, methodDescriptor);

      return switch (methodType) {
        case CLIENT_STREAMING ->
            handleClientStreaming(workflowContext, arguments, methodDescriptor, call);
        case BIDI_STREAMING ->
            handleBidiStreaming(workflowContext, arguments, methodDescriptor, call);
        case SERVER_STREAMING ->
            handleServerStreaming(workflowContext, methodDescriptor, arguments, call);
        case UNARY, UNKNOWN -> handleAsyncUnary(workflowContext, methodDescriptor, arguments, call);
      };

    } catch (Descriptors.DescriptorValidationException | InvalidProtocolBufferException e) {
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

  private static CompletableFuture<WorkflowModel> handleClientStreaming(
      WorkflowContext workflowContext,
      Map<String, Object> parameters,
      Descriptors.MethodDescriptor methodDescriptor,
      ClientCall<Message, Message> call) {
    return ProtobufMessageUtils.asyncStreamingCall(
        parameters,
        methodDescriptor,
        responseObserver -> ClientCalls.asyncClientStreamingCall(call, responseObserver),
        workflowContext.definition().application().modelFactory());
  }

  private static CompletableFuture<WorkflowModel> handleBidiStreaming(
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

  private static CompletableFuture<WorkflowModel> handleServerStreaming(
      WorkflowContext workflowContext,
      Descriptors.MethodDescriptor methodDescriptor,
      Map<String, Object> parameters,
      ClientCall<Message, Message> call)
      throws InvalidProtocolBufferException {
    CollectionStreamObserver observer =
        new CollectionStreamObserver(workflowContext.definition().application().modelFactory());
    ClientCalls.asyncServerStreamingCall(
        call, ProtobufMessageUtils.buildMessage(methodDescriptor, parameters).build(), observer);
    return observer.future();
  }

  private static CompletableFuture<WorkflowModel> handleAsyncUnary(
      WorkflowContext workflowContext,
      Descriptors.MethodDescriptor methodDescriptor,
      Map<String, Object> parameters,
      ClientCall<Message, Message> call)
      throws InvalidProtocolBufferException {

    CompletableFuture<WorkflowModel> future = new CompletableFuture<>();
    ClientCalls.asyncUnaryCall(
        call,
        ProtobufMessageUtils.buildMessage(methodDescriptor, parameters).build(),
        new StreamObserver<>() {
          private WorkflowModel model;

          @Override
          public void onNext(Message value) {
            model =
                ProtobufMessageUtils.convert(
                    value, workflowContext.definition().application().modelFactory());
          }

          @Override
          public void onError(Throwable t) {
            future.completeExceptionally(t);
          }

          @Override
          public void onCompleted() {
            future.complete(model);
          }
        });
    return future;
  }
}
