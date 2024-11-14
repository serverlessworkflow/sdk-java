package io.serverlessworkflow.impl;

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
import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class WorkflowDefinitionTest {

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testWorkflowExecution(String fileName, Object input, Condition<Object> condition)
      throws IOException {
    assertThat(
            WorkflowDefinition.builder(readWorkflowFromClasspath(fileName))
                .build()
                .execute(input)
                .output())
        .is(condition);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "call-http-query-parameters.yaml",
        "call-http-query-parameters-external-schema.yaml"
      })
  void testWrongSchema(String fileName) {
    IllegalArgumentException exception =
        catchThrowableOfType(
            IllegalArgumentException.class,
            () ->
                WorkflowDefinition.builder(readWorkflowFromClasspath(fileName))
                    .build()
                    .execute(Map.of()));
    assertThat(exception)
        .isNotNull()
        .hasMessageContaining("There are JsonSchema validation errors");
  }

  private static Stream<Arguments> provideParameters() {
    Map<String, Object> petInput = Map.of("petId", 10);
    Condition<Object> petCondition =
        new Condition<>(
            o -> ((Map<String, Object>) o).containsKey("photoUrls"), "callHttpCondition");
    return Stream.of(
        Arguments.of("callGetHttp.yaml", petInput, petCondition),
        Arguments.of("call-http-endpoint-interpolation.yaml", petInput, petCondition),
        Arguments.of(
            "call-http-query-parameters.yaml",
            Map.of("searchQuery", "R2-D2"),
            new Condition<>(
                o -> ((Map<String, Object>) o).get("count").equals(1), "R2D2Condition")),
        Arguments.of(
            "call-http-query-parameters-external-schema.yaml",
            Map.of("searchQuery", "Luke Skywalker"),
            new Condition<>(
                o -> ((Map<String, Object>) o).get("count").equals(1), "TheRealJediCondition")),
        Arguments.of(
            "callPostHttp.yaml",
            Map.of("name", "Javierito", "status", "available"),
            new Condition<>(o -> o.equals("Javierito"), "CallHttpPostCondition")));
  }
}
