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
package io.serverlessworkflow.impl.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.events.EventRegistration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LifeCycleEventsTest {

  private WorkflowApplication appl;
  private Collection<CloudEvent> publishedEvents;
  private Collection<EventRegistration> registrations;

  @BeforeEach
  void setup() {
    publishedEvents = new CopyOnWriteArrayList<>();
    appl = WorkflowApplication.builder().build();
    registrations = new ArrayList<>();
    Collection<EventRegistrationBuilder> builders = appl.eventConsumer().listenToAll(appl);

    for (EventRegistrationBuilder builder : builders) {
      registrations.add(
          appl.eventConsumer().register(builder, ce -> publishedEvents.add((CloudEvent) ce)));
    }
  }

  @AfterEach
  void close() {
    registrations.forEach(r -> appl.eventConsumer().unregister(r));
    appl.close();
  }

  @Test
  void simpleWorkflow() throws IOException {

    WorkflowModel model =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("simple-expression.yaml"))
            .instance(Map.of())
            .start()
            .join();
    assertThat(model.asMap()).hasValueSatisfying(m -> assertThat(m).hasSize(3));
    WorkflowStartedCEData workflowStartedEvent =
        assertPojoInCE("io.serverlessworkflow.workflow.started.v1", WorkflowStartedCEData.class);
    TaskStartedCEData taskStartedEvent =
        assertPojoInCE("io.serverlessworkflow.task.started.v1", TaskStartedCEData.class);
    TaskCompletedCEData taskCompletedEvent =
        assertPojoInCE("io.serverlessworkflow.task.completed.v1", TaskCompletedCEData.class);
    WorkflowCompletedCEData workflowCompletedEvent =
        assertPojoInCE(
            "io.serverlessworkflow.workflow.completed.v1", WorkflowCompletedCEData.class);
    assertThat(workflowCompletedEvent.output()).isEqualTo(model.asJavaObject());
    assertThat(workflowStartedEvent.startedAt()).isBefore(workflowCompletedEvent.completedAt());
    assertThat(taskCompletedEvent.output()).isEqualTo(model.asJavaObject());
    assertThat(taskCompletedEvent.completedAt()).isBefore(workflowCompletedEvent.completedAt());
    assertThat(taskStartedEvent.startedAt()).isAfter(workflowStartedEvent.startedAt());
    assertThat(taskStartedEvent.startedAt()).isBefore(taskCompletedEvent.completedAt());
  }

  @Test
  void testSuspendResumeNotWait()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    WorkflowInstance instance =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("wait-set.yaml"))
            .instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();
    instance.suspend();
    instance.resume();
    assertThat(future.get(1, TimeUnit.SECONDS).asMap().orElseThrow())
        .isEqualTo(Map.of("name", "Javierito"));
  }

  @Test
  void testSuspendResumeWait()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    WorkflowInstance instance =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("wait-set.yaml"))
            .instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();
    instance.suspend();
    assertThat(instance.status()).isEqualTo(WorkflowStatus.WAITING);
    Thread.sleep(550);
    assertThat(instance.status()).isEqualTo(WorkflowStatus.SUSPENDED);
    instance.resume();
    assertThat(future.get(1, TimeUnit.SECONDS).asMap().orElseThrow())
        .isEqualTo(Map.of("name", "Javierito"));
    assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
  }

  @Test
  void testCancel() throws IOException, InterruptedException {
    WorkflowInstance instance =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("wait-set.yaml"))
            .instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();
    instance.cancel();
    assertThat(catchThrowableOfType(ExecutionException.class, () -> future.get().asMap()))
        .isNotNull();
    assertThat(instance.status()).isEqualTo(WorkflowStatus.CANCELLED);
  }

  @Test
  void testSuspendResumeTimeout()
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    WorkflowInstance instance =
        appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath("wait-set.yaml"))
            .instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();
    instance.suspend();
    assertThat(catchThrowableOfType(TimeoutException.class, () -> future.get(1, TimeUnit.SECONDS)))
        .isNotNull();
  }

  @Test
  void testError() throws IOException {
    Workflow workflow = WorkflowReader.readWorkflowFromClasspath("raise-inline.yaml");
    assertThat(
            catchThrowableOfType(
                CompletionException.class,
                () -> appl.workflowDefinition(workflow).instance(Map.of()).start().join()))
        .isNotNull();
    WorkflowErrorCEData error =
        assertPojoInCE("io.serverlessworkflow.workflow.faulted.v1", WorkflowFailedCEData.class)
            .error();
    assertThat(error.type()).isEqualTo("https://serverlessworkflow.io/errors/not-implemented");
    assertThat(error.title()).isEqualTo("Not Implemented");
    assertThat(error.status()).isEqualTo(500);
    assertThat(error.detail()).contains("raise-not-implemented");
  }

  private <T> T assertPojoInCE(String type, Class<T> clazz) {
    Thread.yield();
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
