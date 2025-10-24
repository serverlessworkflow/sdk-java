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
import static org.junit.Assert.assertEquals;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class SubWorkflowTest {

  private static final String WORKFLOW_TEST_PATH = "workflows-samples/sub-workflow/";

  @Test
  public void setTest() throws IOException {
    Workflow workflowParent =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-parent.yaml");
    Workflow workflowChild =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-child.yaml");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(workflowChild);
      Map<String, Object> result =
          app.workflowDefinition(workflowParent)
              .instance(Map.of())
              .start()
              .get()
              .asMap()
              .orElseThrow();
      assertEquals("1", result.get("counter").toString());
      assertEquals("helloWorld", result.get("greeting").toString());
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }

  @Test
  public void setBlankInputTest() throws IOException {
    Workflow workflowParent =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-parent.yaml");
    Workflow workflowChild =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-child.yaml");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(workflowChild);
      Map<String, Object> result =
          app.workflowDefinition(workflowParent)
              .instance(Map.of())
              .start()
              .get()
              .asMap()
              .orElseThrow();
      assertEquals("1", result.get("counter").toString());
      assertEquals("helloWorld", result.get("greeting").toString());
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }

  @Test
  public void setStringInputTest() throws IOException {
    Workflow workflowParent =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-parent.yaml");
    Workflow workflowChild =
        readWorkflowFromClasspath(WORKFLOW_TEST_PATH + "sub-workflow-child.yaml");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(workflowChild);
      Map<String, Object> result =
          app.workflowDefinition(workflowParent)
              .instance("Tested")
              .start()
              .get()
              .asMap()
              .orElseThrow();
      assertEquals("1", result.get("counter").toString());
      assertEquals("helloWorld", result.get("greeting").toString());
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }

  @Test
  public void readContextAndSetTest() throws IOException {
    Workflow workflowParent =
        readWorkflowFromClasspath(
            WORKFLOW_TEST_PATH + "read-context-and-set-sub-workflow-parent.yaml");
    Workflow workflowChild =
        readWorkflowFromClasspath(
            WORKFLOW_TEST_PATH + "read-context-and-set-sub-workflow-child.yaml");
    Map<String, Object> map = Map.of("userId", "userId_1", "username", "test", "password", "test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(workflowChild);
      Map<String, Object> result =
          app.workflowDefinition(workflowParent).instance(map).start().get().asMap().orElseThrow();
      Map<String, String> updated = (Map<String, String>) result.get("updated");
      assertEquals("userId_1_tested", updated.get("userId"));
      assertEquals("test_tested", updated.get("username"));
      assertEquals("test_tested", updated.get("password"));
      assertEquals(
          "The workflow set-into-context:1.0.0 updated user in context", result.get("detail"));
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }

  @Test
  public void outputExportContextAndSetTest() throws IOException {
    Workflow workflowParent =
        readWorkflowFromClasspath(
            WORKFLOW_TEST_PATH + "output-export-and-set-sub-workflow-parent.yaml");
    Workflow workflowChild =
        readWorkflowFromClasspath(
            WORKFLOW_TEST_PATH + "output-export-and-set-sub-workflow-child.yaml");
    Map<String, Object> map = Map.of("userId", "userId_1", "username", "test", "password", "test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(workflowChild);
      Map<String, Object> result =
          app.workflowDefinition(workflowParent).instance(map).start().get().asMap().orElseThrow();
      assertEquals("userId_1_tested", result.get("userId"));
      assertEquals("test_tested", result.get("username"));
      assertEquals("test_tested", result.get("password"));
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }
  }
}
