/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.serverlessworkflow.api.test;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.DelayState;
import io.serverlessworkflow.api.workflow.Events;
import io.serverlessworkflow.api.workflow.Functions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.serverlessworkflow.api.states.DefaultState.Type.DELAY;
import static org.junit.jupiter.api.Assertions.*;

public class WorkflowToMarkupTest {
    @Test
    public void testSingleState() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }

    @Test
    public void testSingleFunction() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withFunctions(new Functions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction").withResource("testResource").withType("testType")))
                )
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);
        assertNotNull(workflow.getFunctions());
        assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
        assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }

    @Test
    public void testSingleEvent() {

        Workflow workflow = new Workflow().withId("test-workflow").withName("test-workflow-name").withVersion("1.0")
                .withEvents(new Events(Arrays.asList(
                        new EventDefinition().withName("testEvent").withSource("testSource").withType("testType")
                                .withKind(EventDefinition.Kind.PRODUCED)))
                )
                .withFunctions(new Functions(Arrays.asList(
                        new FunctionDefinition().withName("testFunction").withResource("testResource").withType("testType")))
                )
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        assertNotNull(workflow);
        assertEquals(1, workflow.getStates().size());
        State state = workflow.getStates().get(0);
        assertTrue(state instanceof DelayState);
        assertNotNull(workflow.getFunctions());
        assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
        assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());
        assertNotNull(workflow.getEvents());
        assertEquals(1, workflow.getEvents().getEventDefs().size());
        assertEquals("testEvent", workflow.getEvents().getEventDefs().get(0).getName());
        assertEquals(EventDefinition.Kind.PRODUCED, workflow.getEvents().getEventDefs().get(0).getKind());

        assertNotNull(Workflow.toJson(workflow));
        assertNotNull(Workflow.toYaml(workflow));
    }
}
