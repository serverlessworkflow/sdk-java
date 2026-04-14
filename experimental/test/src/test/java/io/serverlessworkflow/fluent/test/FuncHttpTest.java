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
package io.serverlessworkflow.fluent.test;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.http;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FuncHttpTest {

  private WorkflowApplication app;
  private MockWebServer mockServer;

  @BeforeEach
  void setup() throws IOException {
    app = WorkflowApplication.builder().build();
    mockServer = new MockWebServer();
    mockServer.start(0);
    mockServer.enqueue(new MockResponse(204, Headers.of("Content-Type", "application/json"), ""));
  }

  @AfterEach
  void cleanup() {
    mockServer.close();
    app.close();
  }

  private RecordedRequest takeRequestOrFail() throws Exception {
    RecordedRequest request = mockServer.takeRequest(150, TimeUnit.MILLISECONDS);
    assertThat(request)
        .as("Expected an HTTP request to be received by MockWebServer within 300 ms")
        .isNotNull();
    return request;
  }

  @Test
  @DisplayName("Query method with single key-value pair")
  void test_query_with_single_key_value() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-single")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query("param1", "value1"))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());

    instance.start().join();

    RecordedRequest request = takeRequestOrFail();

    assertSoftly(
        softly -> {
          softly.assertThat(request.getUrl().toString()).contains("param1=value1");
          softly.assertThat(request.getMethod()).isEqualTo("GET");
        });
  }

  @Test
  @DisplayName("Query method with multiple single key-value pairs (individually tested)")
  void test_query_with_multiple_single_values() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-single-multi")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query("param1", "value1")
                    .query("param2", "value2")
                    .query("param3", "value3"))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();
    String url = request.getUrl().toString();

    assertSoftly(
        softly -> {
          softly.assertThat(url).contains("param1=value1").isNotEmpty();
          softly.assertThat(url).contains("param2=value2").isNotEmpty();
          softly.assertThat(url).contains("param3=value3").isNotEmpty();
        });
  }

  @Test
  @DisplayName("Query method with Map of parameters")
  void test_query_with_map() throws Exception {

    var workflow =
        FuncWorkflowBuilder.workflow("test-query-map")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query(Map.of("userId", "123", "userName", "john", "status", "active")))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();
    String url = request.getUrl().toString();

    assertSoftly(
        softly -> {
          softly.assertThat(url).contains("userId=123");
          softly.assertThat(url).contains("userName=john");
          softly.assertThat(url).contains("status=active");
        });
  }

  @Test
  @DisplayName("Query method with expression string")
  void test_query_with_expression() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-expression")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query("enabled", "${ .enabled }"))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of("enabled", true));
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();

    assertThat(request.getUrl().query()).contains("enabled=true");
  }

  @Test
  @DisplayName("Query method with empty Map")
  void test_query_with_empty_map() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-empty-map")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query(Map.of()))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();

    assertSoftly(
        softly -> {
          softly.assertThat(request.getUrl().encodedPath()).isEqualTo("/api/endpoint");
          softly.assertThat(request.getUrl().encodedQuery()).isNull();
        });
  }

  @Test
  @DisplayName("Query method with special characters in values")
  void test_query_with_special_characters() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-special-chars")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query("email", "user@example.com"))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();

    assertSoftly(
        softly -> {
          softly.assertThat(request.getUrl().queryParameter("email")).isEqualTo("user@example.com");
          softly.assertThat(request.getUrl().encodedQuery()).contains("email=user%40example.com");
        });
  }

  @Test
  @DisplayName("Query method overload - Map with multiple values")
  void test_query_map_multiple_values() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-map-multi")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .query(Map.of("limit", "50", "offset", "0", "sort", "name")))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();
    String url = request.getUrl().toString();

    assertSoftly(
        softly -> {
          softly.assertThat(url).contains("limit=50");
          softly.assertThat(url).contains("offset=0");
          softly.assertThat(url).contains("sort=name");
        });
  }

  @Test
  @DisplayName("Query method with headers and query parameters")
  void test_query_with_headers_and_query() throws Exception {
    var workflow =
        FuncWorkflowBuilder.workflow("test-query-with-headers")
            .tasks(
                http("callHttp")
                    .GET()
                    .uri(mockServer.url("/api/endpoint").toString())
                    .header("Authorization", "Bearer token123")
                    .header("Accept", "application/json")
                    .query("userId", "123"))
            .build();

    WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());
    instance.start().join();

    RecordedRequest request = takeRequestOrFail();
    String url = request.getUrl().toString();
    assertThat(url).contains("userId=123");
  }
}
