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
package io.serverlessworkflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventDefinitionTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
  }

  @ParameterizedTest
  @MethodSource("eventListenerParameters")
  void testEventListened(String listen, String emit, JsonNode expectedResult, Object emitInput)
      throws IOException {
    WorkflowDefinition listenDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(listen));
    WorkflowDefinition emitDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(emit));
    WorkflowInstance waitingInstance = listenDefinition.instance(Map.of());
    CompletableFuture<JsonNode> future = waitingInstance.start();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.RUNNING);
    emitDefinition.instance(emitInput).start().join();
    assertThat(future).isCompleted();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    assertThat(waitingInstance.outputAsJsonNode()).isEqualTo(expectedResult);
  }

  private static Stream<Arguments> eventListenerParameters() {
    return Stream.of(
        Arguments.of("listen-to-any.yaml", "emit.yaml", cruellaDeVil(), Map.of()),
        Arguments.of(
            "listen-to-any-filter.yaml", "emit-doctor.yaml", doctor(), Map.of("temperature", 39)));
  }

  private static JsonNode cruellaDeVil() {
    ObjectMapper mapper = JsonUtils.mapper();
    ObjectNode node = mapper.createObjectNode();
    node.set(
        "client", mapper.createObjectNode().put("firstName", "Cruella").put("lastName", "de Vil"));
    node.set(
        "items",
        mapper
            .createArrayNode()
            .add(mapper.createObjectNode().put("breed", "dalmatian").put("quantity", 101)));
    return mapper.createArrayNode().add(node);
  }

  private static JsonNode doctor() {
    ObjectMapper mapper = JsonUtils.mapper();
    ObjectNode node = mapper.createObjectNode();
    node.put("temperature", 39);
    node.put("isSick", true);
    return mapper.createArrayNode().add(node);
  }
}
