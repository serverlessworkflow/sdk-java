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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.fluent.spec.dsl.DSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.Map;
import org.junit.Test;

public class SubWorkflowTest {

  private static final String WORKFLOW_TEST_PATH = "workflows-samples/sub-workflow/";

  @Test
  public void setTest() throws Exception {
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
              .join()
              .asMap()
              .orElseThrow();
      assertThat(result.get("counter"), is(equalTo(1)));
      assertThat(result.get("greeting"), is(equalTo("helloWorld")));
    }
  }

  @Test
  public void setBlankInputTest() throws Exception {
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
              .join()
              .asMap()
              .orElseThrow();
      assertThat(result.get("counter"), is(equalTo(1)));
      assertThat(result.get("greeting"), is(equalTo("helloWorld")));
    }
  }

  @Test
  public void setStringInputTest() throws Exception {
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
              .join()
              .asMap()
              .orElseThrow();
      assertThat(result.get("counter"), is(equalTo(1)));
      assertThat(result.get("greeting"), is(equalTo("helloWorld")));
    }
  }

  @Test
  public void readContextAndSetTest() throws Exception {
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
          app.workflowDefinition(workflowParent).instance(map).start().join().asMap().orElseThrow();
      Map<String, String> updated = (Map<String, String>) result.get("updated");
      assertThat(updated.get("userId"), is(equalTo("userId_1_tested")));
      assertThat(updated.get("username"), is(equalTo("test_tested")));
      assertThat(updated.get("password"), is(equalTo("test_tested")));
      assertThat(
          result.get("detail"),
          is(equalTo("The workflow set-into-context:1.0.0 updated user in context")));
    }
  }

  @Test
  public void outputExportContextAndSetTest() throws Exception {
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
          app.workflowDefinition(workflowParent).instance(map).start().join().asMap().orElseThrow();
      assertThat(result.get("userId"), is(equalTo("userId_1_tested")));
      assertThat(result.get("username"), is(equalTo("test_tested")));
      assertThat(result.get("password"), is(equalTo("test_tested")));
    }
  }

  @Test
  public void runSubWorkflowFromDslTest() throws Exception {
    Workflow child =
        WorkflowBuilder.workflow("childFlow", "org.acme", "1.0.0")
            .tasks(d -> d.set("update", s -> s.put("counter", 1).put("greeting", "helloWorld")))
            .build();

    Workflow parent =
        WorkflowBuilder.workflow("parentFlow", "org.acme", "1.0.0")
            .tasks(
                DSL.workflow(
                    DSL.workflow("org.acme", "childFlow", "1.0.0")
                        .input(Map.of("id", 42, "region", "us-east"))))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.workflowDefinition(child);
      Map<String, Object> result =
          app.workflowDefinition(parent).instance(Map.of()).start().join().asMap().orElseThrow();
      assertThat(result.get("counter"), is(equalTo(1)));
      assertThat(result.get("greeting"), is(equalTo("helloWorld")));
    }
  }
}
