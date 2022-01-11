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
package io.serverlessworkflow.diagram.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowDiagram;
import io.serverlessworkflow.diagram.WorkflowDiagramImpl;
import io.serverlessworkflow.diagram.test.utils.DiagramTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CustomTemplateWorkflowDiagramTest {

  @ParameterizedTest
  @ValueSource(strings = {"/examples/applicantrequest.json", "/examples/applicantrequest.yml"})
  public void testSpecExamplesParsing(String workflowLocation) throws Exception {

    Workflow workflow = Workflow.fromSource(DiagramTestUtils.readWorkflowFile(workflowLocation));

    assertNotNull(workflow);
    assertNotNull(workflow.getId());
    assertNotNull(workflow.getName());
    assertNotNull(workflow.getStates());

    WorkflowDiagram workflowDiagram =
        new WorkflowDiagramImpl()
            .showLegend(true)
            .setWorkflow(workflow)
            .setTemplate("custom-template");

    String diagramSVG = workflowDiagram.getSvgDiagram();

    Assertions.assertNotNull(diagramSVG);
    // custom template uses customcolor in the legend
    Assertions.assertTrue(diagramSVG.contains("customcolor"));
  }
}
