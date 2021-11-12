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
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class EventsTest {
  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetConsumedEvents(String workflowEvents) {
    int expectedEventsCount = 2;
    Collection<String> expectedConsumedEvent =
        Arrays.asList("SATScoresReceived", "RecommendationLetterReceived");
    Set<String> uniqueExpectedConsumedEvent = new HashSet<>(expectedConsumedEvent);
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    List<EventDefinition> consumedEvents = WorkflowUtils.getWorkflowConsumedEvents(workflow);
    assertEquals(expectedEventsCount, consumedEvents.size());
    for (EventDefinition consumedEvent : consumedEvents) {
      assertTrue(uniqueExpectedConsumedEvent.contains(consumedEvent.getName()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetConsumedEventsCount(String workflowEvents) {
    int expectedEventsCount = 2;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    int workflowConsumedEventsCount = WorkflowUtils.getWorkflowConsumedEventsCount(workflow);
    Arrays.asList(expectedEventsCount, workflowConsumedEventsCount);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithproducedevents.yml"})
  public void testGetWorkflowProducedEvents(String workflowProducedEvents) {
    int expectedEventsCount = 1;
    Collection<String> expectedProducedEvent = Arrays.asList("ApplicationSubmitted");
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowProducedEvents);
    List<EventDefinition> producedEvents = WorkflowUtils.getWorkflowProducedEvents(workflow);
    assertNotNull(producedEvents);
    assertEquals(expectedEventsCount, producedEvents.size());
    for (EventDefinition producedEvent : producedEvents) {
      assertTrue(expectedProducedEvent.contains(producedEvent.getName()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithproducedevents.yml"})
  public void testGetWorkflowProducedEventsCount(String workflowProducedEvents) {
    int expectedEventsCount = 1;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowProducedEvents);
    int producedEventsCount = WorkflowUtils.getWorkflowProducedEventsCount(workflow);
    assertEquals(expectedEventsCount, producedEventsCount);
  }
}
