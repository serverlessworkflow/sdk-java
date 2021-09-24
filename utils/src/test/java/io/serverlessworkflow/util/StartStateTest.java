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

import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.util.testutil.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

@TestWithResources
class StartStateTest {

    @GivenTextResource("/start/workflowwithstartstate.yml")
    static String WORKFLOW_WITH_START_SPECIFIED;

    @GivenTextResource("/start/workflowwithstartnotspecified.yml")
    static String WORKFLOW_WITH_START_NOT_SPECIFIED;

    @Test
    public void testGetStartState() {
        Workflow workflow = TestUtils.createWorkflow(WORKFLOW_WITH_START_SPECIFIED);
        Optional<State> startingState = Workflows.getStartingState(workflow);
        Assertions.assertTrue(startingState.isPresent());
        Assertions.assertEquals(startingState.get().getName(), workflow.getStart().getStateName());
    }

    @Test
    public void testGetStartStateForWorkflowWithStartNotSpecified() {
        Workflow workflow = TestUtils.createWorkflow(WORKFLOW_WITH_START_NOT_SPECIFIED);
        Optional<State> startingState = Workflows.getStartingState(workflow);
        Assertions.assertTrue(startingState.isPresent());
        Assertions.assertEquals(workflow.getStates().get(0).getName(), startingState.get().getName());
    }

    @Test
    public void testGetStateForNullWorkflow() {
        Assertions.assertThrows(NullPointerException.class, () -> Workflows.getStartingState(null));
    }
}

