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
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.states.EventState;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GetStatesTest {

  @ParameterizedTest
  @ValueSource(strings = {"/getStates/workflowwithstates.yml"})
  public void testGetStatesByDefaultState(String workflowWithState) {
    int matchingEvents = 2;
    int notMatchingEvents = 0;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithState);
    List<State> matchingStates = WorkflowUtils.getStates(workflow, DefaultState.Type.EVENT);
    List<State> notMatchingStates = WorkflowUtils.getStates(workflow, DefaultState.Type.SLEEP);
    assertEquals(matchingEvents, matchingStates.size());
    assertEquals(notMatchingEvents, notMatchingStates.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/getStates/workflowwithstates.yml"})
  public void getStateByName(String workflowWithState) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithState);

    State finalizeApplicationState =
        WorkflowUtils.getStateWithName(workflow, "FinalizeApplication");
    assertNotNull(finalizeApplicationState);
    assertTrue(finalizeApplicationState instanceof EventState);

    State cancelApplicationState = WorkflowUtils.getStateWithName(workflow, "CancelApplication");
    assertNotNull(cancelApplicationState);
    assertTrue(cancelApplicationState instanceof EventState);
  }

  @Test
  public void testGetsStatesForNullWorkflow() {
    assertNull(WorkflowUtils.getStates(null, DefaultState.Type.EVENT));
  }
}
