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
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StartStateTest {

  @ParameterizedTest
  @ValueSource(strings = {"/start/workflowwithstartstate.yml"})
  public void testGetStartState(String workflowWithStartState) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithStartState);
    State startingState = WorkflowUtils.getStartingState(workflow);
    assertNotNull(startingState);
    assertEquals(startingState.getName(), workflow.getStart().getStateName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/start/workflowwithstartnotspecified.yml"})
  public void testGetStartStateForWorkflowWithStartNotSpecified(
      String workflowWithStartStateNotSpecified) {
    Workflow workflow =
        TestUtils.createWorkflowFromTestResource(workflowWithStartStateNotSpecified);
    State startingState = WorkflowUtils.getStartingState(workflow);
    assertEquals(workflow.getStates().get(0).getName(), startingState.getName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/start/workflowwithnostate.yml"})
  public void testGetStartStateForWorkflowWithNoState(String workflowWithNoState) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithNoState);
    State startingState = WorkflowUtils.getStartingState(workflow);
    assertNull(startingState);
  }

  @Test
  public void testGetStateForNullWorkflow() {
    State startingState = WorkflowUtils.getStartingState(null);
    assertNull(startingState);
  }
}
