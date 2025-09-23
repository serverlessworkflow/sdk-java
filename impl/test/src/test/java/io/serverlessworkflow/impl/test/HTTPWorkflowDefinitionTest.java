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

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class HTTPWorkflowDefinitionTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void cleanup() {
    appl.close();
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testWorkflowExecution(String fileName, Object input, Condition<Object> condition)
      throws IOException {
    assertThat(
            appl.workflowDefinition(readWorkflowFromClasspath(fileName))
                .instance(input)
                .start()
                .join())
        .is(condition);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "workflows-samples/call-http-query-parameters.yaml",
        "workflows-samples/call-http-query-parameters-external-schema.yaml"
      })
  void testWrongSchema(String fileName) {
    IllegalArgumentException exception =
        catchThrowableOfType(
            IllegalArgumentException.class,
            () -> appl.workflowDefinition(readWorkflowFromClasspath(fileName)).instance(Map.of()));
    assertThat(exception)
        .isNotNull()
        .hasMessageContaining("There are JsonSchema validation errors");
  }

  private static boolean httpCondition(WorkflowModel obj) {
    Map<String, Object> map = obj.asMap().orElseThrow();
    return map.containsKey("photoUrls") || map.containsKey("petId");
  }

  private static Stream<Arguments> provideParameters() {
    Map<String, Object> petInput = Map.of("petId", 10);
    Map<String, Object> starTrekInput = Map.of("uid", "MOMA0000092393");
    Condition<WorkflowModel> petCondition =
        new Condition<>(HTTPWorkflowDefinitionTest::httpCondition, "callHttpCondition");
    Condition<WorkflowModel> starTrekCondition =
        new Condition<>(
            o ->
                ((Map<String, Object>) o.asMap().orElseThrow().get("movie"))
                    .get("title")
                    .equals("Star Trek"),
            "StartTrek");
    return Stream.of(
        Arguments.of("workflows-samples/callGetHttp.yaml", petInput, petCondition),
        Arguments.of(
            "workflows-samples/callGetHttp.yaml",
            Map.of("petId", "-1"),
            new Condition<WorkflowModel>(
                o -> o.asMap().orElseThrow().containsKey("petId"), "notFoundCondition")),
        Arguments.of(
            "workflows-samples/call-http-endpoint-interpolation.yaml", petInput, petCondition),
        Arguments.of(
            "workflows-samples/call-http-query-parameters.yaml", starTrekInput, starTrekCondition),
        Arguments.of(
            "workflows-samples/call-http-query-parameters-external-schema.yaml",
            starTrekInput,
            starTrekCondition),
        Arguments.of(
            "workflows-samples/callPostHttp.yaml",
            Map.of("name", "Javierito", "surname", "Unknown"),
            new Condition<WorkflowModel>(
                o -> o.asText().orElseThrow().equals("Javierito"), "CallHttpPostCondition")));
  }
}
