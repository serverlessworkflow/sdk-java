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

package io.serverlessworkflow.util;

import static org.junit.jupiter.api.Assertions.*;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FunctionDefinitionsTest {

  @ParameterizedTest
  @ValueSource(strings = {"/funcdefinitiontest/functiondefinition.yml"})
  public void testFunctionDefsForAction(String funcDefinitions) {
    String actionLookUp = "finalizeApplicationAction";
    String expectedFunctionRefName = "finalizeApplicationFunction";
    Workflow workflow = TestUtils.createWorkflowFromTestResource(funcDefinitions);
    FunctionDefinition finalizeApplicationFunctionDefinition =
        WorkflowUtils.getFunctionDefinitionsForAction(workflow, actionLookUp);
    assertNotNull(finalizeApplicationFunctionDefinition);
    assertEquals(expectedFunctionRefName, finalizeApplicationFunctionDefinition.getName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/funcdefinitiontest/functiondefinition.yml"})
  public void testFunctionDefsForActionNotPresent(String funcDefinitions) {
    String actionLookUp = "finalizeApplicationFunctionNotPresent";
    Workflow workflow = TestUtils.createWorkflowFromTestResource(funcDefinitions);
    FunctionDefinition finalizeApplicationFunctionDefinition =
        WorkflowUtils.getFunctionDefinitionsForAction(workflow, actionLookUp);
    assertNull(finalizeApplicationFunctionDefinition);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/funcdefinitiontest/functiondefinition.yml"})
  public void testFunctionDefsForNullWorkflow(String funcDefinitions) {
    assertNull(WorkflowUtils.getFunctionDefinitionsForAction(null, "TestAction"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/funcdefinitiontest/functiondefinition.yml"})
  public void testGetActionsForFunctionDefinition(String funcDefinitions) {
    String functionRefName = "finalizeApplicationFunction";
    int expectedActionCount = 2;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(funcDefinitions);
    List<Action> actionsForFunctionDefinition =
        WorkflowUtils.getActionsForFunctionDefinition(workflow, functionRefName);
    assertEquals(expectedActionCount, actionsForFunctionDefinition.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/funcdefinitiontest/functiondefinition.yml"})
  public void testGetFunctionDefinitionWithName(String funcDefinitions) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(funcDefinitions);
    assertNotNull(
        WorkflowUtils.getFunctionDefinitionWithName(workflow, "finalizeApplicationFunction"));
  }
}
