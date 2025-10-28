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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ContainerTest {

  @Test
  public void testContainer() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/container/container.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertNotNull(result);
  }
}
