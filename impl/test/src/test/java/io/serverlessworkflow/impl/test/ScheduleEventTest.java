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

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ScheduleEventTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() throws IOException {
    appl = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void tearDown() throws IOException {
    appl.close();
  }

  @Test
  void testStartUsingEvent() throws IOException, InterruptedException, ExecutionException {
    appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/listen-start.yaml"));
    appl.eventPublishers().forEach(p -> p.publish(buildCloudEvent(Map.of("name", "Javierito"))));
    Collection<WorkflowInstance> instances = appl.scheduler().scheduledInstances();
    await()
        .pollDelay(Duration.ofMillis(10))
        .atMost(Duration.ofMillis(200))
        .until(() -> instances.size() == 1);
    appl.eventPublishers().forEach(p -> p.publish(buildCloudEvent(Map.of("name", "Fulanito"))));
    await()
        .pollDelay(Duration.ofMillis(10))
        .atMost(Duration.ofMillis(200))
        .until(() -> instances.size() == 2);
    List<Object> outputs = instances.stream().map(i -> i.output().asJavaObject()).toList();
    assertThat(outputs.get(0)).isEqualTo(Map.of("recovered", "Javierito"));
    assertThat(outputs.get(1)).isEqualTo(Map.of("recovered", "Fulanito"));
  }

  private CloudEvent buildCloudEvent(Object data) {
    return CloudEventBuilder.v1()
        .withId("1")
        .withType("com.example.hospital.events.patients.recover")
        .withSource(URI.create("http://www.fakejavieritotest.com"))
        .withData(JsonCloudEventData.wrap(JsonUtils.fromValue(data)))
        .build();
  }
}
