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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

public class SecretExpressionTest {

  private static Workflow workflow;

  @BeforeAll
  static void init() throws IOException {
    workflow = readWorkflowFromClasspath("workflows-samples/secret-expression.yaml");
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void testDefault() {
    System.setProperty("superman.name", "ClarkKent");
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      assertThat(
              appl.workflowDefinition(workflow)
                  .instance(Map.of())
                  .start()
                  .join()
                  .asMap()
                  .orElseThrow()
                  .get("superSecret"))
          .isEqualTo("ClarkKent");
    } finally {
      System.clearProperty("superman.name");
    }
  }

  @Test
  void testMissing() {
    try (WorkflowApplication appl = WorkflowApplication.builder().build()) {
      assertThatThrownBy(
              () ->
                  appl.workflowDefinition(workflow)
                      .instance(Map.of())
                      .start()
                      .join()
                      .asMap()
                      .orElseThrow())
          .hasCauseInstanceOf(WorkflowException.class);
    }
  }

  @Test
  void testCustom() {
    try (WorkflowApplication appl =
        WorkflowApplication.builder().withSecretManager(k -> Map.of("name", "ClarkKent")).build()) {
      assertThat(
              appl.workflowDefinition(workflow)
                  .instance(Map.of())
                  .start()
                  .join()
                  .asMap()
                  .orElseThrow()
                  .get("superSecret"))
          .isEqualTo("ClarkKent");
    }
  }
}
