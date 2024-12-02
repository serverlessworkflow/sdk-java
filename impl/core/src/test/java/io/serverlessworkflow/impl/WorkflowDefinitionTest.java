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
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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
  void testWorkflowExecution(String fileName, Consumer<WorkflowDefinition> assertions)
      throws IOException {
    assertions.accept(appl.workflowDefinition(readWorkflowFromClasspath(fileName)));
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        args(
            "switch-then-string.yaml",
            Map.of("orderType", "electronic"),
            o ->
                assertThat(o.output())
                    .isEqualTo(
                        Map.of(
                            "orderType", "electronic", "validate", true, "status", "fulfilled"))),
        args(
            "switch-then-string.yaml",
            Map.of("orderType", "physical"),
            o ->
                assertThat(o.output())
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
                assertThat(o.output())
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
            o -> assertThat(o.output()).isEqualTo(6)),
        args(
            "for-collect.yaml",
            Map.of("input", Arrays.asList(1, 2, 3)),
            o ->
                assertThat(o.output())
                    .isEqualTo(
                        Map.of("input", Arrays.asList(1, 2, 3), "output", Arrays.asList(2, 4, 6)))),
        args(
            "simple-expression.yaml",
            Map.of("input", Arrays.asList(1, 2, 3)),
            WorkflowDefinitionTest::checkSpecialKeywords),
        args(
            "raise-inline copy.yaml",
            WorkflowDefinitionTest::checkWorkflowException,
            WorkflowException.class),
        args(
            "raise-reusable.yaml",
            WorkflowDefinitionTest::checkWorkflowException,
            WorkflowException.class));
  }

  private static Arguments args(
      String fileName, Map<String, Object> input, Consumer<WorkflowInstance> instance) {
    return Arguments.of(
        fileName, (Consumer<WorkflowDefinition>) d -> instance.accept(d.execute(input)));
  }

  private static <T extends Throwable> Arguments args(
      String fileName, Consumer<T> consumer, Class<T> clazz) {
    return Arguments.of(
        fileName,
        (Consumer<WorkflowDefinition>)
            d -> consumer.accept(catchThrowableOfType(clazz, () -> d.execute(Map.of()))));
  }

  private static void checkWorkflowException(WorkflowException ex) {
    assertThat(ex.getWorflowError().type())
        .isEqualTo("https://serverlessworkflow.io/errors/not-implemented");
    assertThat(ex.getWorflowError().status()).isEqualTo(500);
    assertThat(ex.getWorflowError().title()).isEqualTo("Not Implemented");
    assertThat(ex.getWorflowError().details()).contains("raise-not-implemented");
    assertThat(ex.getWorflowError().instance()).isEqualTo("do/0/notImplemented");
  }

  private static void checkSpecialKeywords(WorkflowInstance obj) {
    Map<String, Object> result = (Map<String, Object>) obj.output();
    assertThat(Instant.ofEpochMilli((long) result.get("startedAt")))
        .isAfterOrEqualTo(before)
        .isBeforeOrEqualTo(Instant.now());
    assertThat(result.get("id").toString()).hasSize(26);
    assertThat(result.get("version").toString()).contains("alpha");
  }
}
