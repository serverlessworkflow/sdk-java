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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RetryTimeoutTest {

  private static WorkflowApplication app;
  private MockWebServer apiServer;

  @BeforeAll
  static void init() {
    app = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void cleanup() {
    app.close();
  }

  @BeforeEach
  void setUp() throws IOException {
    apiServer = new MockWebServer();
    apiServer.start(9797);
  }

  @AfterEach
  void tearDown() throws IOException {
    apiServer.shutdown();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "workflows-samples/try-catch-retry-inline.yaml",
        "workflows-samples/try-catch-retry-reusable.yaml"
      })
  void testRetry(String path) throws IOException {
    final JsonNode result = JsonUtils.mapper().createObjectNode().put("name", "Javierito");
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(JsonUtils.mapper().writeValueAsString(result)));
    CompletableFuture<WorkflowModel> future =
        app.workflowDefinition(readWorkflowFromClasspath(path))
            .instance(Map.of("delay", 0.01))
            .start();
    Awaitility.await()
        .atMost(Duration.ofSeconds(1))
        .until(() -> future.join().as(JsonNode.class).orElseThrow().equals(result));
  }

  @Test
  void testRetryEnd() throws IOException {
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    apiServer.enqueue(new MockResponse().setResponseCode(404));
    assertThatThrownBy(
            () ->
                app.workflowDefinition(
                        readWorkflowFromClasspath(
                            "workflows-samples/try-catch-retry-reusable.yaml"))
                    .instance(Map.of())
                    .start()
                    .join())
        .hasCauseInstanceOf(WorkflowException.class);
  }

  @Test
  void testTimeout() throws IOException {
    Map<String, Object> result =
        app.workflowDefinition(
                readWorkflowFromClasspath("workflows-samples/listen-to-one-timeout.yaml"))
            .instance(Map.of("delay", 0.01f))
            .start()
            .join()
            .asMap()
            .orElseThrow();
    assertThat(result.get("message")).isEqualTo("Viva er Beti Balompie");
  }

  @Test
  void testCustomFunction() {
    assertThatThrownBy(
            () ->
                app.workflowDefinition(
                        readWorkflowFromClasspath(
                            "workflows-samples/call-custom-function-inline.yaml"))
                    .instance(Map.of())
                    .start()
                    .join())
        .hasCauseInstanceOf(WorkflowException.class)
        .extracting(w -> ((WorkflowException) w.getCause()).getWorkflowError().status())
        .isEqualTo(404);
  }
}
