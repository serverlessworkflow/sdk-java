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

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.events.EventPublisher;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class EmitCloudEventDefaultsTest {

  private static final class CapturingPublisher implements EventPublisher {
    private final List<CloudEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<Void> publish(CloudEvent event) {
      events.add(event);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void publishLifeCycle(CloudEvent event) {
      // ignore lifecycle events for this test
    }

    @Override
    public void close() {}
  }

  private static CloudEvent emitAndCapture(WorkflowApplication.Builder builder, String workflow)
      throws IOException {
    CapturingPublisher publisher = new CapturingPublisher();
    try (WorkflowApplication appl =
        builder.disableLifeCycleCEPublishing().withEventPublisher(publisher).build()) {
      WorkflowDefinition definition =
          appl.workflowDefinition(WorkflowReader.readWorkflowFromClasspath(workflow));
      definition.instance(Map.of("user", Map.of("firstName", "Cruella"))).start().join();
      assertThat(publisher.events).hasSize(1);
      return publisher.events.get(0);
    }
  }

  @Test
  void emit_without_source_derives_source_from_workflow_identity() throws IOException {
    OffsetDateTime before = OffsetDateTime.now();
    CloudEvent event =
        emitAndCapture(WorkflowApplication.builder(), "workflows-samples/emit-defaults.yaml");
    OffsetDateTime after = OffsetDateTime.now();

    // id is always auto-generated
    assertThat(event.getId()).isNotBlank();
    // type is user-provided
    assertThat(event.getType()).isEqualTo("com.acme.order.placed.v1");
    // source is derived from namespace:name:version of the emitting workflow
    assertThat(event.getSource()).isEqualTo(URI.create("acme:emit-defaults:2.1.0"));
    // time now defaults to "now" instead of being omitted
    assertThat(event.getTime()).isNotNull();
    assertThat(event.getTime()).isBetween(before, after);
  }

  @Test
  void application_default_source_overrides_identity_derived_source() throws IOException {
    CloudEvent event =
        emitAndCapture(
            WorkflowApplication.builder().withDefaultEventSource("urn:acme:orders"),
            "workflows-samples/emit-defaults.yaml");

    assertThat(event.getSource()).isEqualTo(URI.create("urn:acme:orders"));
    assertThat(event.getTime()).isNotNull();
  }

  @Test
  void explicit_per_emit_source_overrides_application_default() throws IOException {
    // emit.yaml sets an explicit source: https://petstore.com
    CloudEvent event =
        emitAndCapture(
            WorkflowApplication.builder().withDefaultEventSource("urn:acme:orders"),
            "workflows-samples/emit.yaml");

    assertThat(event.getSource()).isEqualTo(URI.create("https://petstore.com"));
  }
}
