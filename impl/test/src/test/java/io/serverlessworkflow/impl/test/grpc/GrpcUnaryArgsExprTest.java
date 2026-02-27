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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.test.grpc.handlers.ContributorUnaryArgsExprHandler;
import io.serverlessworkflow.impl.test.junit.DisabledIfBinaryUnavailable;
import java.io.IOException;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DisabledIfBinaryUnavailable({"protoc", "--version"})
public class GrpcUnaryArgsExprTest {

  private static final int PORT_FOR_EXAMPLES = 5011;
  private static WorkflowApplication app;
  private static Server server;

  @BeforeAll
  static void setUpApp() throws IOException {
    server =
        ServerBuilder.forPort(PORT_FOR_EXAMPLES)
            .addService(new ContributorUnaryArgsExprHandler())
            .build();
    server.start();

    app = WorkflowApplication.builder().build();
  }

  @AfterEach
  void cleanup() throws InterruptedException {
    server.shutdown().awaitTermination();
  }

  @Test
  void grpcPerson() throws IOException {

    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/grpc/contributors-unary-args-expr-call.yaml");

    WorkflowDefinition workflowDefinition = app.workflowDefinition(workflow);

    Map<String, Object> output =
        workflowDefinition
            .instance(Map.of("github", "bootable[origin]"))
            .start()
            .join()
            .asMap()
            .orElseThrow();

    Assertions.assertThat(output).contains(Map.entry("message", "Success with bootable[origin]"));
  }
}
