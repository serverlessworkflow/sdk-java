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
package io.serverlessworkflow.impl.test.grpc;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowDefinitionId;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.test.grpc.handlers.PersonUnaryHandler;
import io.serverlessworkflow.impl.test.junit.DisabledIfProtocUnavailable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisabledIfProtocUnavailable
public class GrpcUnaryTest {

  private static final int PORT_FOR_EXAMPLES = 5011;
  private WorkflowApplication app;
  private Server server;

  @BeforeEach
  void setUp() throws IOException {
    server = ServerBuilder.forPort(PORT_FOR_EXAMPLES).addService(new PersonUnaryHandler()).build();
    server.start();

    app = WorkflowApplication.builder().build();
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    if (server != null) {
      server.shutdownNow();
      server.awaitTermination(10, TimeUnit.SECONDS);
    }
    if (app != null) {
      app.close();
    }
  }

  @Test
  void grpcPerson() throws IOException {

    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/grpc/get-person-call.yaml");

    WorkflowDefinition workflowDefinition = app.workflowDefinition(workflow);

    String protoFilePath =
        java.util.Objects.requireNonNull(
                getClass()
                    .getClassLoader()
                    .getResource("workflows-samples/grpc/proto/person.proto"))
            .toString();

    Map<String, Object> output =
        workflowDefinition
            .instance(Map.of("protoFilePath", protoFilePath))
            .start()
            .join()
            .asMap()
            .orElseThrow();

    Assertions.assertThat(output).contains(Map.entry("name", "John Doe"), Map.entry("id", 891182));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("getPersonCallSources")
  void testGetPersonCallDsl(String sourceName, Workflow workflow) throws IOException {
    String protoFilePath =
        java.util.Objects.requireNonNull(
                getClass()
                    .getClassLoader()
                    .getResource("workflows-samples/grpc/proto/person.proto"))
            .toString();

    Map<String, Object> output =
        app.workflowDefinition(workflow)
            .instance(Map.of("protoFilePath", protoFilePath))
            .start()
            .thenApply(
                model -> (Map<String, Object>) JsonUtils.toJavaValue(JsonUtils.modelToJson(model)))
            .join();

    assertThat(output).contains(Map.entry("name", "John Doe"), Map.entry("id", 891182));
  }

  private static Stream<Arguments> getPersonCallSources() throws IOException {
    return Stream.of(
            readWorkflowFromClasspath("workflows-samples/grpc/get-person-call.yaml"),
            getPersonCallWorkflow())
        .map(w -> Arguments.of(WorkflowDefinitionId.of(w).toString(), w));
  }

  private static Workflow getPersonCallWorkflow() {
    return WorkflowBuilder.workflow("grpc-example", "test", "0.1.0")
        .tasks(
            doTasks(
                call(
                    "greet",
                    grpc()
                        .proto("workflows-samples/grpc/proto/person.proto")
                        .service("Person", "localhost", PORT_FOR_EXAMPLES)
                        .method("GetPerson"))))
        .build();
  }
}
