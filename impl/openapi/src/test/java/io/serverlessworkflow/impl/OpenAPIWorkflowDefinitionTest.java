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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenAPIWorkflowDefinitionTest {

  private static WorkflowApplication app;

  @BeforeAll
  static void init() {
    app = WorkflowApplication.builder().build();
  }

  @Test
  void testWorkflowExecution() throws IOException {
    Object output =
        app.workflowDefinition(readWorkflowFromClasspath("findPetsByStatus.yaml"))
            .execute(Map.of("status", "sold"))
            .outputAsJsonNode();
    assertThat(output)
        .isInstanceOf(JsonNode.class)
        .satisfies(
            (obj) -> {
              JsonNode json = (JsonNode) obj;
              assertThat(json.get("status").asText()).isEqualTo("sold");
            });
  }
}
