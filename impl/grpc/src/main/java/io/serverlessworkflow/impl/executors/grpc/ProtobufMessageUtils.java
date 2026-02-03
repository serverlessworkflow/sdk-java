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
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import io.serverlessworkflow.api.WorkflowFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public interface ProtobufMessageUtils {

  static JsonNode convert(Message message) {
    StringBuilder str = new StringBuilder();
    try {
      JsonFormat.printer().appendTo(message, str);
      return WorkflowFormat.JSON.mapper().readTree(str.toString());
    } catch (IOException e) {
      throw new UncheckedIOException("Error converting protobuf message to JSON", e);
    }
  }

  static MethodDescriptor.MethodType getMethodType(
      com.google.protobuf.Descriptors.MethodDescriptor methodDesc) {
    DescriptorProtos.MethodDescriptorProto methodDescProto = methodDesc.toProto();
    if (methodDescProto.getClientStreaming()) {
      if (methodDescProto.getServerStreaming()) {
        return MethodDescriptor.MethodType.BIDI_STREAMING;
      }
      return MethodDescriptor.MethodType.CLIENT_STREAMING;
    } else if (methodDescProto.getServerStreaming()) {
      return MethodDescriptor.MethodType.SERVER_STREAMING;
    } else {
      return MethodDescriptor.MethodType.UNARY;
    }
  }

  static JsonNode asyncStreamingCall(
      Map<String, Object> parameters,
      com.google.protobuf.Descriptors.MethodDescriptor methodDescriptor,
      UnaryOperator<StreamObserver<Message>> streamObserverFunction,
      Function<List<JsonNode>, JsonNode> nodesFunction) {
    WaitingStreamObserver responseObserver = new WaitingStreamObserver();
    StreamObserver<Message> requestObserver = streamObserverFunction.apply(responseObserver);

    for (var entry : parameters.entrySet()) {
      try {
        Message message =
            buildMessage(entry, DynamicMessage.newBuilder(methodDescriptor.getInputType())).build();
        requestObserver.onNext(message);
      } catch (Exception e) {
        requestObserver.onError(e);
        throw new RuntimeException(e);
      }
      responseObserver.checkForServerStreamErrors();
    }
    requestObserver.onCompleted();

    return nodesFunction.apply(
        responseObserver.get().stream()
            .map(ProtobufMessageUtils::convert)
            .collect(Collectors.toList()));
  }

  static Message.Builder buildMessage(Object object, Message.Builder builder)
      throws InvalidProtocolBufferException, JsonProcessingException {
    JsonFormat.parser().merge(WorkflowFormat.JSON.mapper().writeValueAsString(object), builder);
    return builder;
  }

  static Message.Builder buildMessage(
      Descriptors.MethodDescriptor methodDescriptor, Map<String, Object> parameters)
      throws InvalidProtocolBufferException, JsonProcessingException {
    DynamicMessage.Builder builder = DynamicMessage.newBuilder(methodDescriptor.getInputType());
    JsonFormat.parser().merge(WorkflowFormat.JSON.mapper().writeValueAsString(parameters), builder);
    return builder;
  }
}
