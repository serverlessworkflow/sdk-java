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

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WorkflowDefinitionTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testWorkflowExecution(String fileName, Object input, Condition<Object> condition)
      throws IOException {
    assertThat(appl.workflowDefinition(readWorkflowFromClasspath(fileName)).execute(input).output())
        .is(condition);
  }

  private static Stream<Arguments> provideParameters() {
    Map<String, Object> petInput = Map.of("petId", 10);
    Condition<Object> petCondition =
        new Condition<>(
            o -> ((Map<String, Object>) o).containsKey("photoUrls"), "callHttpCondition");
    return Stream.of(
        Arguments.of(
            "switch-then-string.yaml",
            Map.of("orderType", "electronic"),
            new Condition(
                o ->
                    o.equals(
                        Map.of("orderType", "electronic", "validate", true, "status", "fulfilled")),
                "switch-electronic")),
        Arguments.of(
            "switch-then-string.yaml",
            Map.of("orderType", "physical"),
            new Condition(
                o ->
                    o.equals(
                        Map.of(
                            "orderType",
                            "physical",
                            "inventory",
                            "clear",
                            "items",
                            1,
                            "address",
                            "Elmer St")),
                "switch-physical")),
        Arguments.of(
            "switch-then-string.yaml",
            Map.of("orderType", "unknown"),
            new Condition(
                o ->
                    o.equals(
                        Map.of(
                            "orderType", "unknown", "log", "warn", "message", "something's wrong")),
                "switch-unknown")));
  }
}
