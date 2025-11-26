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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.Headers;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HTTPWorkflowDefinitionTest {

  private static WorkflowApplication appl;
  private static MockWebServer mockServer;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void cleanup() {
    appl.close();
  }

  @BeforeEach
  void setup() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start(9876);
  }

  @AfterEach
  void shutdownServer() {
    mockServer.close();
  }

  private static boolean httpCondition(WorkflowModel obj) {
    Map<String, Object> map = obj.asMap().orElseThrow();
    return map.containsKey("photoUrls") || map.containsKey("petId");
  }

  @Test
  void callHttpGet_should_return_pet_data() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/call-http-get.yaml"))
            .instance(Map.of("petId", 10))
            .start()
            .join();
    assertThat(result)
        .has(new Condition<>(HTTPWorkflowDefinitionTest::httpCondition, "callHttpCondition"));
  }

  @Test
  void callHttpGet_with_not_found_petId_should_keep_input_petId() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/call-http-get.yaml"))
            .instance(Map.of("petId", "-1"))
            .start()
            .join();
    assertThat(result.asMap().orElseThrow()).containsKey("petId");
  }

  @Test
  void callHttpEndpointInterpolation_should_work() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(
                readWorkflowFromClasspath(
                    "workflows-samples/call-http-endpoint-interpolation.yaml"))
            .instance(Map.of("petId", 10))
            .start()
            .join();
    assertThat(result)
        .has(new Condition<>(HTTPWorkflowDefinitionTest::httpCondition, "callHttpCondition"));
  }

  @Test
  void callHttpQueryParameters_should_find_star_trek_movie() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(
                readWorkflowFromClasspath("workflows-samples/call-http-query-parameters.yaml"))
            .instance(Map.of("uid", "MOMA0000092393"))
            .start()
            .join();
    assertThat(((Map<String, Object>) result.asMap().orElseThrow().get("movie")).get("title"))
        .isEqualTo("Star Trek");
  }

  @Test
  void callHttpFindByStatus_should_return_non_empty_collection() throws Exception {

    WorkflowModel result =
        appl.workflowDefinition(
                readWorkflowFromClasspath("workflows-samples/call-http-find-by-status.yaml"))
            .instance(Map.of())
            .start()
            .join();
    assertThat(result.asCollection()).isNotEmpty();
  }

  @Test
  void callHttpQueryParameters_external_schema_should_find_star_trek() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(
                readWorkflowFromClasspath(
                    "workflows-samples/call-http-query-parameters-external-schema.yaml"))
            .instance(Map.of("uid", "MOMA0000092393"))
            .start()
            .join();
    assertThat(((Map<String, Object>) result.asMap().orElseThrow().get("movie")).get("title"))
        .isEqualTo("Star Trek");
  }

  @Test
  void callHttpPost_should_return_created_firstName() throws Exception {
    mockServer.enqueue(
        new MockResponse(
            200,
            Headers.of("Content-Type", "application/json"),
            """
                                    {
                                        "firstName": "Javierito"
                                    }
                                    """));
    WorkflowModel result =
        appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/call-http-post.yaml"))
            .instance(Map.of("name", "Javierito", "surname", "Unknown"))
            .start()
            .join();
    assertThat(result.asText().orElseThrow()).isEqualTo("Javierito");
  }

  @Test
  void testCallHttpDelete() {
    assertDoesNotThrow(
        () -> {
          appl.workflowDefinition(
                  readWorkflowFromClasspath("workflows-samples/call-http-delete.yaml"))
              .instance(Map.of())
              .start()
              .join();
        });
  }

  @Test
  void callHttpPut_should_contain_firstName_with_john() throws Exception {
    WorkflowModel result =
        appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/call-http-put.yaml"))
            .instance(Map.of("firstName", "John"))
            .start()
            .join();
    assertThat(result.asText().orElseThrow()).contains("John");
  }

  @Test
  void testWrongSchema_should_throw_illegal_argument() {
    IllegalArgumentException exception =
        catchThrowableOfType(
            IllegalArgumentException.class,
            () ->
                appl.workflowDefinition(
                        readWorkflowFromClasspath(
                            "workflows-samples/call-http-query-parameters.yaml"))
                    .instance(Map.of()));
    assertThat(exception)
        .isNotNull()
        .hasMessageContaining("There are JsonSchema validation errors");
  }

  @Test
  void testHeadCall() {
    mockServer.enqueue(
        new MockResponse(
            200,
            Headers.of(
                Map.of(
                    "Content-Length",
                    "123",
                    "Content-Type",
                    "application/json",
                    "X-Custom-Header",
                    "CustomValue")),
            ""));
    assertDoesNotThrow(
        () -> {
          appl.workflowDefinition(
                  readWorkflowFromClasspath("workflows-samples/call-http-head.yaml"))
              .instance(Map.of())
              .start()
              .join();
        });
  }

  @Test
  void testOptionsCall() {
    mockServer.enqueue(new MockResponse(200, Headers.of("Allow", "GET, POST, OPTIONS"), ""));

    assertDoesNotThrow(
        () -> {
          appl.workflowDefinition(
                  readWorkflowFromClasspath("workflows-samples/call-http-options.yaml"))
              .instance(Map.of())
              .start()
              .join();
        });
  }

  @Test
  void testRedirectAsFalse() {
    mockServer.enqueue(
        new MockResponse(301, Headers.of("Location", "http://localhost:9876/redirected"), ""));

    CompletionException exception =
        catchThrowableOfType(
            CompletionException.class,
            () ->
                appl.workflowDefinition(
                        readWorkflowFromClasspath(
                            "workflows-samples/call-http-redirect-false.yaml"))
                    .instance(Map.of())
                    .start()
                    .join());

    assertThat(exception.getCause().getMessage())
        .contains(
            "The property 'redirect' is set to false but received status 301 (Redirection); expected status in the 200-299 range");
  }
}
