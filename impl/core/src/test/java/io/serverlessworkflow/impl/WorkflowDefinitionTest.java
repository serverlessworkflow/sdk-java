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
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WorkflowDefinitionTest {

  private static WorkflowApplication appl;
  private static Instant before;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
    before = Instant.now();
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testWorkflowExecution(String fileName, Object input, Consumer<Object> assertions)
      throws IOException {
    assertions.accept(
        appl.workflowDefinition(readWorkflowFromClasspath(fileName)).execute(input).output());
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        args(
            "switch-then-string.yaml",
            Map.of("orderType", "electronic"),
            o ->
                assertThat(o)
                    .isEqualTo(
                        Map.of(
                            "orderType", "electronic", "validate", true, "status", "fulfilled"))),
        args(
            "switch-then-string.yaml",
            Map.of("orderType", "physical"),
            o ->
                assertThat(o)
                    .isEqualTo(
                        Map.of(
                            "orderType",
                            "physical",
                            "inventory",
                            "clear",
                            "items",
                            1,
                            "address",
                            "Elmer St"))),
        args(
            "switch-then-string.yaml",
            Map.of("orderType", "unknown"),
            o ->
                assertThat(o)
                    .isEqualTo(
                        Map.of(
                            "orderType",
                            "unknown",
                            "log",
                            "warn",
                            "message",
                            "something's wrong"))),
        args(
            "for-sum.yaml",
            Map.of("input", Arrays.asList(1, 2, 3)),
            o -> assertThat(o).isEqualTo(6)),
        args(
            "for-collect.yaml",
            Map.of("input", Arrays.asList(1, 2, 3)),
            o ->
                assertThat(o)
                    .isEqualTo(
                        Map.of("input", Arrays.asList(1, 2, 3), "output", Arrays.asList(2, 4, 6)))),
        args(
            "simple-expression.yaml",
            Map.of("input", Arrays.asList(1, 2, 3)),
            WorkflowDefinitionTest::checkSpecialKeywords));
  }

  private static Arguments args(
      String fileName, Map<String, Object> input, Consumer<Object> object) {
    return Arguments.of(fileName, input, object);
  }

  private static void checkSpecialKeywords(Object obj) {
    Map<String, Object> result = (Map<String, Object>) obj;
    assertThat(Instant.ofEpochMilli((long) result.get("startedAt")))
        .isAfterOrEqualTo(before)
        .isBeforeOrEqualTo(Instant.now());
    assertThat(result.get("id").toString()).hasSize(26);
    assertThat(result.get("version").toString()).contains("alpha");
  }
}
