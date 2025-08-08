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
package io.serverlessworkflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.events.EventRegistrationBuilder;
import io.serverlessworkflow.impl.lifecycle.ce.TaskCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.TaskStartedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowCompletedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowErrorCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowFailedCEData;
import io.serverlessworkflow.impl.lifecycle.ce.WorkflowStartedCEData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifeCycleEventsTest {

  private static WorkflowApplication appl;
  private static Collection<CloudEvent> publishedEvents;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
    appl.eventConsumer()
        .listenToAll(appl)
        .forEach(
            v ->
                appl.eventConsumer()
                    .register(
                        (EventRegistrationBuilder) v, ce -> publishedEvents.add((CloudEvent) ce)));
  }

  @AfterAll
  static void cleanup() {
    appl.close();
  }

  @BeforeEach
  void setup() {
    publishedEvents = new ArrayList<>();
  }

  @AfterEach
  void close() {
    publishedEvents = new ArrayList<>();
  }

  @Test
  void simpleWorkflow() throws IOException {
    WorkflowModel model =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("simple-expression.yaml"))
            .instance(Map.of())
            .start()
            .join();
    WorkflowCompletedCEData workflowCompletedEvent =
        assertPojoInCE(
            "io.serverlessworkflow.workflow.completed.v1", WorkflowCompletedCEData.class);
    assertThat(workflowCompletedEvent.output()).isEqualTo(model.asJavaObject());
    WorkflowStartedCEData workflowStartedEvent =
        assertPojoInCE("io.serverlessworkflow.workflow.started.v1", WorkflowStartedCEData.class);
    assertThat(workflowStartedEvent.startedAt()).isBefore(workflowCompletedEvent.completedAt());
    TaskCompletedCEData taskCompletedEvent =
        assertPojoInCE("io.serverlessworkflow.task.completed.v1", TaskCompletedCEData.class);
    assertThat(taskCompletedEvent.output()).isEqualTo(model.asJavaObject());
    assertThat(taskCompletedEvent.completedAt()).isBefore(workflowCompletedEvent.completedAt());
    TaskStartedCEData taskStartedEvent =
        assertPojoInCE("io.serverlessworkflow.task.started.v1", TaskStartedCEData.class);
    assertThat(taskStartedEvent.startedAt()).isAfter(workflowStartedEvent.startedAt());
    assertThat(taskStartedEvent.startedAt()).isBefore(taskCompletedEvent.completedAt());
  }

  @Test
  void testError() throws IOException {
    appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("raise-inline.yaml"))
        .instance(Map.of())
        .start();
    WorkflowErrorCEData error =
        assertPojoInCE("io.serverlessworkflow.workflow.faulted.v1", WorkflowFailedCEData.class)
            .error();
    assertThat(error.type()).isEqualTo("https://serverlessworkflow.io/errors/not-implemented");
    assertThat(error.title()).isEqualTo("Not Implemented");
    assertThat(error.status()).isEqualTo(500);
    assertThat(error.detail()).contains("raise-not-implemented");
  }

  private <T> T assertPojoInCE(String type, Class<T> clazz) {
    return assertPojoInCE(type, clazz, 1L);
  }

  private <T> T assertPojoInCE(String type, Class<T> clazz, long count) {
    assertThat(publishedEvents.stream().filter(ev -> ev.getType().equals(type)).count())
        .isEqualTo(count);
    Optional<CloudEvent> event =
        publishedEvents.stream().filter(ev -> ev.getType().equals(type)).findAny();
    assertThat(event)
        .hasValueSatisfying(ce -> assertThat(ce.getData()).isInstanceOf(PojoCloudEventData.class));
    assertThat(event)
        .hasValueSatisfying(
            ce -> assertThat(((PojoCloudEventData) ce.getData()).getValue()).isInstanceOf(clazz));
    return clazz.cast(((PojoCloudEventData) event.orElseThrow().getData()).getValue());
  }
}
