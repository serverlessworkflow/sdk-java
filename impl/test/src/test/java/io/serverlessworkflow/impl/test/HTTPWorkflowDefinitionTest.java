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
import java.util.stream.Stream;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.Headers;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @MethodSource("provideParameters")
  void testWorkflowExecution(
      String fileName, Object input, Runnable setup, Condition<Object> condition)
      throws IOException {
    setup.run();
    assertThat(
            appl.workflowDefinition(readWorkflowFromClasspath(fileName))
                .instance(input)
                .start()
                .join())
        .is(condition);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "workflows-samples/call-http-query-parameters.yaml",
        "workflows-samples/call-http-query-parameters-external-schema.yaml"
      })
  void testWrongSchema(String fileName) {
    IllegalArgumentException exception =
        catchThrowableOfType(
            IllegalArgumentException.class,
            () -> appl.workflowDefinition(readWorkflowFromClasspath(fileName)).instance(Map.of()));
    assertThat(exception)
        .isNotNull()
        .hasMessageContaining("There are JsonSchema validation errors");
  }

  private static boolean httpCondition(WorkflowModel obj) {
    Map<String, Object> map = obj.asMap().orElseThrow();
    return map.containsKey("photoUrls") || map.containsKey("petId");
  }

  private static Stream<Arguments> provideParameters() {
    Map<String, Object> petInput = Map.of("petId", 10);
    Map<String, Object> starTrekInput = Map.of("uid", "MOMA0000092393");
    Condition<WorkflowModel> petCondition =
        new Condition<>(HTTPWorkflowDefinitionTest::httpCondition, "callHttpCondition");
    Condition<WorkflowModel> starTrekCondition =
        new Condition<>(
            o ->
                ((Map<String, Object>) o.asMap().orElseThrow().get("movie"))
                    .get("title")
                    .equals("Star Trek"),
            "StartTrek");
    Condition<WorkflowModel> postCondition =
        new Condition<WorkflowModel>(
            o -> o.asText().orElseThrow().equals("Javierito"), "CallHttpPostCondition");

    Condition<WorkflowModel> putCondition =
        new Condition<>(o -> o.asText().get().contains("John"), "CallHttpPutCondition");

    Condition<WorkflowModel> patchCondition =
        new Condition<>(o -> o.asText().get().contains("John"), "CallHttpPatchCondition");

    Map<String, String> postMap = Map.of("name", "Javierito", "surname", "Unknown");
    Map<String, Object> putMap = Map.of("firstName", "John");

    Runnable setupPost =
        () ->
            mockServer.enqueue(
                new MockResponse(
                    200,
                    Headers.of("Content-Type", "application/json"),
                    """
                        {
                            "firstName": "Javierito"
                        }
                        """));

    return Stream.of(
        Arguments.of("workflows-samples/call-http-get.yaml", petInput, doNothing, petCondition),
        Arguments.of(
            "workflows-samples/call-http-get.yaml",
            Map.of("petId", "-1"),
            doNothing,
            new Condition<WorkflowModel>(
                o -> o.asMap().orElseThrow().containsKey("petId"), "notFoundCondition")),
        Arguments.of(
            "workflows-samples/call-http-endpoint-interpolation.yaml",
            petInput,
            doNothing,
            petCondition),
        Arguments.of(
            "workflows-samples/call-http-query-parameters.yaml",
            starTrekInput,
            doNothing,
            starTrekCondition),
        Arguments.of(
            "workflows-samples/call-http-find-by-status.yaml",
            Map.of(),
            doNothing,
            new Condition<WorkflowModel>(o -> !o.asCollection().isEmpty(), "HasElementCondition")),
        Arguments.of(
            "workflows-samples/call-http-query-parameters-external-schema.yaml",
            starTrekInput,
            doNothing,
            starTrekCondition),
        Arguments.of("workflows-samples/call-http-post.yaml", postMap, setupPost, postCondition),
        Arguments.of(
            "workflows-samples/call-http-delete.yaml",
            Map.of(),
            doNothing,
            new Condition<WorkflowModel>(o -> o.asMap().isEmpty(), "HTTP delete")),
        Arguments.of("workflows-samples/call-http-put.yaml", putMap, doNothing, putCondition));
  }

  private static final Runnable doNothing = () -> {};

  @Test
  void post_should_run_call_http_with_expression_body() {
    mockServer.enqueue(
        new MockResponse(
            200,
            Headers.of("Content-Type", "application/json"),
            """
                        {
                            "firstName": "Javierito"
                        }
                        """));

    assertDoesNotThrow(
        () -> {
          appl.workflowDefinition(
                  readWorkflowFromClasspath("workflows-samples/call-http-post-expr.yaml"))
              .instance(Map.of("name", "Javierito", "surname", "Unknown"))
              .start()
              .join();
        });
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

  //  @Test
  //  void testRedirectAsTrueWhenReceivingRedirection() {
  //    mockServer.enqueue(
  //        new MockResponse(301, Headers.of("Location", "http://localhost:9876/redirected"), ""));
  //
  //    mockServer.enqueue(
  //        new MockResponse(
  //            200, Headers.of("Content-Type", "application/json"), "{\"status\":\"OK\"}"));
  //
  //    assertDoesNotThrow(
  //        () -> {
  //          appl.workflowDefinition(
  //
  // readWorkflowFromClasspath("workflows-samples/call-with-response-output-expr.yaml"))
  //              .instance(Map.of())
  //              .start()
  //              .join();
  //        });
  //  }
}
