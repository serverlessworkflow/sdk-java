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
package io.serverlessworkflow.impl.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventDefinitionTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().disableLifeCycleCEPublishing().build();
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
    CompletableFuture<WorkflowModel> future = waitingInstance.start();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    emitDefinition.instance(emitInput).start().join();
    assertThat(future).isCompleted();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    assertThat(waitingInstance.outputAs(JsonNode.class)).isEqualTo(expectedResult);
  }

  @ParameterizedTest
  @MethodSource("eventsListenerParameters")
  void testEventsListened(String listen, String emit1, String emit2, JsonNode expectedResult)
      throws IOException {
    WorkflowDefinition listenDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(listen));
    WorkflowDefinition emitDoctorDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(emit1));
    WorkflowDefinition emitOutDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(emit2));
    WorkflowInstance waitingInstance = listenDefinition.instance(Map.of());
    CompletableFuture<WorkflowModel> future = waitingInstance.start();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    emitDoctorDefinition.instance(Map.of("temperature", 35)).start().join();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    emitDoctorDefinition.instance(Map.of("temperature", 39)).start().join();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    emitOutDefinition.instance(Map.of()).start().join();
    assertThat(future).isCompleted();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    assertThat(waitingInstance.outputAs(JsonNode.class)).isEqualTo(expectedResult);
  }

  @Test
  void testForEachInAnyIsExecutedAsEventArrive() throws IOException, InterruptedException {
    WorkflowDefinition listenDefinition =
        appl.workflowDefinition(
            WorkflowReader.readWorkflowFromClasspath("listen-to-any-until.yaml"));
    WorkflowDefinition emitDoctorDefinition =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("emit-doctor.yaml"));
    WorkflowInstance waitingInstance = listenDefinition.instance(Map.of());
    CompletableFuture<WorkflowModel> future = waitingInstance.start();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    emitDoctorDefinition.instance(Map.of("temperature", 35)).start().join();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.WAITING);
    Thread.sleep(1100);
    emitDoctorDefinition.instance(Map.of("temperature", 39)).start().join();
    assertThat(future).isCompleted();
    assertThat(waitingInstance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    ArrayNode result = waitingInstance.outputAs(ArrayNode.class);
    assertThat(ChronoUnit.SECONDS.between(getInstant(result, 0), getInstant(result, 1)))
        .isGreaterThanOrEqualTo(1L);
  }

  private static Instant getInstant(ArrayNode result, int index) {
    return Instant.ofEpochSecond(result.get(index).get("time").asLong());
  }

  private static Stream<Arguments> eventListenerParameters() {
    return Stream.of(
        Arguments.of("listen-to-any.yaml", "emit.yaml", array(cruellaDeVil()), Map.of()),
        Arguments.of(
            "listen-to-any-filter.yaml", "emit-doctor.yaml", doctor(), Map.of("temperature", 39)));
  }

  private static Stream<Arguments> eventsListenerParameters() {
    return Stream.of(
        Arguments.of(
            "listen-to-all.yaml",
            "emit-doctor.yaml",
            "emit.yaml",
            array(temperature(), cruellaDeVil())),
        Arguments.of(
            "listen-to-any-until-consumed.yaml",
            "emit-doctor.yaml",
            "emit-out.yaml",
            array(temperature())));
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
    return node;
  }

  private static JsonNode doctor() {
    ObjectNode node = temperature();
    node.put("isSick", true);
    return array(node);
  }

  private static ObjectNode temperature() {
    ObjectNode node = JsonUtils.mapper().createObjectNode();
    node.put("temperature", 39);
    return node;
  }

  private static JsonNode array(JsonNode... jsonNodes) {
    ArrayNode arrayNode = JsonUtils.mapper().createArrayNode();
    for (JsonNode node : jsonNodes) arrayNode.add(node);
    return arrayNode;
  }
}
