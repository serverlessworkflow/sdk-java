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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GetNumTests {
  @ParameterizedTest
  @ValueSource(strings = {"/getStates/workflowwithstates.yml"})
  public void testGetNumStates(String workflowWithStates) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithStates);
    int expectedStatesCount = 2;
    assertEquals(expectedStatesCount, WorkflowUtils.getNumOfStates(workflow));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/start/workflowwithnostate.yml"})
  public void testGetNumStatesForNoStateInWorkflow(String workflowWithStates) {
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowWithStates);
    int expectedStatesCount = 0;
    assertEquals(expectedStatesCount, WorkflowUtils.getNumOfStates(workflow));
  }
}
