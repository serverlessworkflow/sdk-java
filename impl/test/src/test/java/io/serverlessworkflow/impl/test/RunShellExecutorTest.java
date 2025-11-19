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

import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.ProcessResult;
import java.io.IOException;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(value = OS.LINUX)
public class RunShellExecutorTest {

  @Test
  void testEcho() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("Hello, anonymous");
          });
    }
  }

  @Test
  void testEchoWithJqExpression() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo-jq.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          appl.workflowDefinition(workflow)
              .instance(new Input(new User("John Doe")))
              .start()
              .join();
      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("Hello, John Doe");
          });
    }
  }

  @Test
  void testEchoWithEnvironment() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo-with-env.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          appl.workflowDefinition(workflow).instance(Map.of("lastName", "Doe")).start().join();
      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("Hello John Doe from env!");
          });
    }
  }

  @Test
  void testTouchAndCat() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/touch-cat.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          appl.workflowDefinition(workflow).instance(Map.of("lastName", "Doe")).start().join();
      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("hello world");
          });
    }
  }

  @Test
  void testMissingShellCommand() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/run-shell/missing-shell-command.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      SoftAssertions.assertSoftly(
          softly -> {
            softly
                .assertThatThrownBy(
                    () -> {
                      appl.workflowDefinition(workflow).instance(Map.of()).start().join();
                    })
                .hasMessageContaining("Missing shell command in RunShell task configuration");
          });
    }
  }

  @Test
  void testAwaitBehavior() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/run-shell/echo-not-awaiting.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      Map<String, String> inputMap = Map.of("full_name", "Matheus Cruz");
      WorkflowModel outputModel =
          appl.workflowDefinition(workflow).instance(inputMap).start().join();
      SoftAssertions.assertSoftly(
          softly -> {
            softly.assertThat(outputModel.asMap().get()).isEqualTo(inputMap);
          });
    }
  }

  @Test
  void testStderr() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo-stderr.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      Map<String, String> inputMap = Map.of();

      WorkflowModel outputModel =
          appl.workflowDefinition(workflow).instance(inputMap).start().join();

      SoftAssertions.assertSoftly(
          softly -> {
            softly.assertThat(outputModel.asText()).isPresent();
            softly.assertThat(outputModel.asText().get()).isNotEmpty();
            softly.assertThat(outputModel.asText().get()).contains("ls:");
          });
    }
  }

  @Test
  void testExitCode() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo-exitcode.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      Map<String, String> inputMap = Map.of();

      WorkflowModel outputModel =
          appl.workflowDefinition(workflow).instance(inputMap).start().join();

      SoftAssertions.assertSoftly(
          softly -> {
            softly.assertThat(outputModel.asNumber()).isPresent();
            softly.assertThat(outputModel.asNumber().get()).isNotEqualTo(0);
          });
    }
  }

  @Test
  void testNone() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath("workflows-samples/run-shell/echo-none.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      Map<String, String> inputMap = Map.of();

      WorkflowModel outputModel =
          appl.workflowDefinition(workflow).instance(inputMap).start().join();

      SoftAssertions.assertSoftly(
          softly -> {
            softly.assertThat(outputModel.asJavaObject()).isNull();
          });
    }
  }

  @Test
  void testEchoWithArgsOnlyKey() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/run-shell/echo-with-args-only-key.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          appl.workflowDefinition(workflow)
              .instance(Map.of("firstName", "John", "lastName", "Doe"))
              .start()
              .join();
      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("Hello John Doe from args!");
          });
    }
  }

  @Test
  void testEchoWithArgsKeyValue() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/run-shell/echo-with-args-key-value.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();

      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("--user=john --password=doe");
          });
    }
  }

  @Test
  void testEchoWithArgsKeyValueJq() throws IOException {
    Workflow workflow =
        WorkflowReader.readWorkflowFromClasspath(
            "workflows-samples/run-shell/echo-with-args-key-value-jq.yaml");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          appl.workflowDefinition(workflow)
              .instance(
                  Map.of(
                      "user", "john",
                      "passwordKey", "--password"))
              .start()
              .join();

      SoftAssertions.assertSoftly(
          softly -> {
            ProcessResult result = model.as(ProcessResult.class).orElseThrow();
            softly.assertThat(result.code()).isEqualTo(0);
            softly.assertThat(result.stderr()).isEmpty();
            softly.assertThat(result.stdout()).contains("--user=john --password=doe");
          });
    }
  }

  record Input(User user) {}

  record User(String name) {}
}
