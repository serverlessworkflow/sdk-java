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
import static org.junit.jupiter.api.Assertions.assertNull;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.util.testutil.TestUtils;
import io.serverlessworkflow.utils.WorkflowUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefinedEventsTest {
  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetDefinedConsumedEvents(String workflowEvents) {
    int consumedEventsCount = 2;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    List<EventDefinition> consumedEvents = WorkflowUtils.getDefinedConsumedEvents(workflow);
    assertEquals(consumedEventsCount, consumedEvents.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetDefinedroducedEvents(String workflowEvents) {
    int producedEventsCounts = 1;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    List<EventDefinition> producedEvents = WorkflowUtils.getDefinedProducedEvents(workflow);
    assertEquals(producedEventsCounts, producedEvents.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetDefinedConsumedEventsCount(String workflowEvents) {
    int consumedEventsCountExpected = 2;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    int consumedEventsCount = WorkflowUtils.getDefinedConsumedEventsCount(workflow);
    assertEquals(consumedEventsCountExpected, consumedEventsCount);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/events/workflowwithevents.yml"})
  public void testGetDefinedroducedEventsCount(String workflowEvents) {
    int producedEventsCountExpected = 1;
    Workflow workflow = TestUtils.createWorkflowFromTestResource(workflowEvents);
    int producedEventsCount = WorkflowUtils.getDefinedProducedEventsCount(workflow);
    assertEquals(producedEventsCountExpected, producedEventsCount);
  }

  @Test
  public void testGetDefinedEventsForNullWorkflow() {
    assertNull(WorkflowUtils.getDefinedEvents(null, EventDefinition.Kind.CONSUMED));
  }

  @Test
  public void testGetDefinedEventsCountForNullWorkflow() {
    int expectedCount = 0;
    assertEquals(
        expectedCount, WorkflowUtils.getDefinedEventsCount(null, EventDefinition.Kind.PRODUCED));
  }
}
