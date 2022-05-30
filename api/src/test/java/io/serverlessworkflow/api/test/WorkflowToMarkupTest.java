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
package io.serverlessworkflow.api.test;

import static io.serverlessworkflow.api.states.DefaultState.Type.SLEEP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.auth.AuthDefinition;
import io.serverlessworkflow.api.auth.BasicAuthDefinition;
import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.produce.ProduceEvent;
import io.serverlessworkflow.api.schedule.Schedule;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.switchconditions.DataCondition;
import io.serverlessworkflow.api.switchconditions.EventCondition;
import io.serverlessworkflow.api.transitions.Transition;
import io.serverlessworkflow.api.workflow.Auth;
import io.serverlessworkflow.api.workflow.Constants;
import io.serverlessworkflow.api.workflow.Events;
import io.serverlessworkflow.api.workflow.Functions;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class WorkflowToMarkupTest {
  @Test
  public void testSingleState() {

    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withStart(new Start().withSchedule(new Schedule().withInterval("PT1S")))
            .withConstants(new Constants("constantsValues.json"))
            .withStates(
                Arrays.asList(
                    new SleepState()
                        .withName("sleepState")
                        .withType(SLEEP)
                        .withEnd(
                            new End()
                                .withTerminate(true)
                                .withCompensate(true)
                                .withProduceEvents(
                                    Arrays.asList(new ProduceEvent().withEventRef("someEvent"))))
                        .withDuration("PT1M")));

    assertNotNull(workflow);
    assertNotNull(workflow.getStart());
    Constants constants = workflow.getConstants();
    assertNotNull(constants);
    assertEquals("constantsValues.json", constants.getRefValue());
    assertEquals(1, workflow.getStates().size());
    State state = workflow.getStates().get(0);
    assertTrue(state instanceof SleepState);
    assertNotNull(state.getEnd());
    assertNotNull(Workflow.toJson(workflow));
    assertNotNull(Workflow.toYaml(workflow));
  }

  @Test
  public void testSingleFunction() {

    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withStart(new Start())
            .withFunctions(
                new Functions(
                    Arrays.asList(
                        new FunctionDefinition()
                            .withName("testFunction")
                            .withOperation("testSwaggerDef#testOperationId"))))
            .withStates(
                Arrays.asList(
                    new SleepState()
                        .withName("delayState")
                        .withType(SLEEP)
                        .withEnd(new End())
                        .withDuration("PT1M")));

    assertNotNull(workflow);
    assertNotNull(workflow.getStart());
    assertEquals(1, workflow.getStates().size());
    State state = workflow.getStates().get(0);
    assertTrue(state instanceof SleepState);
    assertNotNull(workflow.getFunctions());
    assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
    assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());

    assertNotNull(Workflow.toJson(workflow));
    assertNotNull(Workflow.toYaml(workflow));
  }

  @Test
  public void testSingleEvent() {

    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withStart(new Start())
            .withEvents(
                new Events(
                    Arrays.asList(
                        new EventDefinition()
                            .withName("testEvent")
                            .withSource("testSource")
                            .withType("testType")
                            .withKind(EventDefinition.Kind.PRODUCED))))
            .withFunctions(
                new Functions(
                    Arrays.asList(
                        new FunctionDefinition()
                            .withName("testFunction")
                            .withOperation("testSwaggerDef#testOperationId"))))
            .withStates(
                Arrays.asList(
                    new SleepState()
                        .withName("delayState")
                        .withType(SLEEP)
                        .withEnd(new End())
                        .withDuration("PT1M")));

    assertNotNull(workflow);
    assertNotNull(workflow.getStart());
    assertEquals(1, workflow.getStates().size());
    State state = workflow.getStates().get(0);
    assertTrue(state instanceof SleepState);
    assertNotNull(workflow.getFunctions());
    assertEquals(1, workflow.getFunctions().getFunctionDefs().size());
    assertEquals("testFunction", workflow.getFunctions().getFunctionDefs().get(0).getName());
    assertNotNull(workflow.getEvents());
    assertEquals(1, workflow.getEvents().getEventDefs().size());
    assertEquals("testEvent", workflow.getEvents().getEventDefs().get(0).getName());
    assertEquals(
        EventDefinition.Kind.PRODUCED, workflow.getEvents().getEventDefs().get(0).getKind());

    assertNotNull(Workflow.toJson(workflow));
    assertNotNull(Workflow.toYaml(workflow));
  }

  @Test
  public void testAuth() {
    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withStart(new Start())
            .withAuth(
                new Auth(
                    new AuthDefinition()
                        .withName("authname")
                        .withScheme(AuthDefinition.Scheme.BASIC)
                        .withBasicauth(
                            new BasicAuthDefinition()
                                .withUsername("testuser")
                                .withPassword("testPassword"))));

    assertNotNull(workflow);
    assertNotNull(workflow.getAuth());
    assertNotNull(workflow.getAuth().getAuthDefs().get(0));
    assertEquals("authname", workflow.getAuth().getAuthDefs().get(0).getName());
    assertNotNull(workflow.getAuth().getAuthDefs().get(0).getScheme());
    assertEquals("basic", workflow.getAuth().getAuthDefs().get(0).getScheme().value());
    assertNotNull(workflow.getAuth().getAuthDefs().get(0).getBasicauth());
    assertEquals("testuser", workflow.getAuth().getAuthDefs().get(0).getBasicauth().getUsername());
    assertEquals(
        "testPassword", workflow.getAuth().getAuthDefs().get(0).getBasicauth().getPassword());
  }

  @Test
  public void testSwitchConditionWithTransition() {
    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withSpecVersion("0.8")
            .withStates(
                Arrays.asList(
                    new SwitchState()
                        .withDataConditions(
                            Arrays.asList(
                                new DataCondition()
                                    .withCondition("test-condition")
                                    .withTransition(
                                        new Transition().withNextState("test-next-state"))))
                        .withDefaultCondition(
                            new DefaultConditionDefinition()
                                .withTransition(new Transition("test-next-state-default")))));
    assertNotNull(Workflow.toJson(workflow));
    assertNotNull(Workflow.toYaml(workflow));
  }

  @Test
  public void testSwitchConditionWithEnd() {
    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withName("test-workflow-name")
            .withVersion("1.0")
            .withSpecVersion("0.8")
            .withStates(
                Arrays.asList(
                    new SwitchState()
                        .withEventConditions(Arrays.asList(new EventCondition().withEnd(new End())))
                        .withDefaultCondition(
                            new DefaultConditionDefinition().withEnd(new End()))));
    assertNotNull(Workflow.toJson(workflow));
    assertNotNull(Workflow.toYaml(workflow));
  }
}
