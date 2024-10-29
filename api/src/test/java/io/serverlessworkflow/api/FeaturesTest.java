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
package io.serverlessworkflow.api;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflow;
import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static io.serverlessworkflow.api.WorkflowWriter.writeWorkflow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.Workflow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FeaturesTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "features/callHttp.yaml",
        "features/callOpenAPI.yaml",
        "features/composite.yaml",
        "features/data-flow.yaml",
        "features/emit.yaml",
        "features/flow.yaml",
        "features/for.yaml",
        "features/raise.yaml",
        "features/set.yaml",
        "features/switch.yaml",
        "features/try.yaml",
        "features/listen.yaml",
        "features/callFunction.yaml",
        "features/callCustomFunction.yaml"
      })
  public void testSpecFeaturesParsing(String workflowLocation) throws IOException {
    Workflow workflow = readWorkflowFromClasspath(workflowLocation);
    assertWorkflow(workflow);
    assertWorkflow(writeAndReadInMemory(workflow));
  }

  private static Workflow writeAndReadInMemory(Workflow workflow) throws IOException {
    byte[] bytes;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeWorkflow(out, workflow, WorkflowFormat.JSON);
      bytes = out.toByteArray();
    }
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      return readWorkflow(in, WorkflowFormat.JSON);
    }
  }

  private static void assertWorkflow(Workflow workflow) {
    assertNotNull(workflow);
    assertNotNull(workflow.getDocument());
    assertNotNull(workflow.getDo());
  }
}
