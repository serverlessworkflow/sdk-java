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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenAPITest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer authServer;
  private MockWebServer openApiServer;
  private MockWebServer restServer;

  private OkHttpClient httpClient;

  private static String PROJECT_JSON_SUCCESS =
      """
                  {
                      "success": true,
                      "data": {
                          "id": 55504,
                          "name": "CRM",
                          "code": "crm-20251111",
                          "ownerId": 12345,
                          "members": [
                              12345,
                              67890
                          ],
                          "created_at": "2025-09-20T00:58:50.170784Z"
                      }
                  }
                  """;

  private static String PROJECT_JSON_FALSE =
      """
                  {
                      "success": false,
                      "error": {
                          "code": "PROJECT_CONFLICT",
                          "message": "A project with the code "crm-2025" already exists.",
                          "details": null
                      }
                  }
                  """;

  private static String PROJECT_GET_JSON_POSITIVE =
      """
                  {
                    "success": true,
                    "data": {
                      "id": 40099,
                      "name": "Severus Calix",
                      "email": "severus.calix@hive-terra.example.com"
                    },
                    "meta": {
                      "request_id": "req_terra123def456",
                      "timestamp": "999.M41-01-20T12:00:00Z"
                    }
                  }
                  """;

  @BeforeEach
  void setUp() throws IOException {
    authServer = new MockWebServer();
    authServer.start(8888);

    openApiServer = new MockWebServer();
    openApiServer.start(8887);

    restServer = new MockWebServer();
    restServer.start(8886);

    httpClient = new OkHttpClient();
  }

  @AfterEach
  void tearDown() throws IOException {
    authServer.shutdown();
    openApiServer.shutdown();
    restServer.shutdown();
  }

  @Test
  public void testOpenAPIBearerQueryInlinedBodyWithPositiveResponse() throws Exception {
    Workflow workflow =
        readWorkflowFromClasspath("workflows-samples/openapi/project-post-positive.yaml");

    URL url = this.getClass().getResource("/schema/openapi/openapi.yaml");

    Path workflowPath = Path.of(url.getPath());
    String yaml = Files.readString(workflowPath, StandardCharsets.UTF_8);

    openApiServer.enqueue(
        new MockResponse()
            .setBody(yaml)
            .setHeader("Content-Type", "application/yaml")
            .setResponseCode(200));

    restServer.enqueue(
        new MockResponse()
            .setBody(PROJECT_JSON_SUCCESS)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(201));

    Map<String, Object> result;

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    RecordedRequest restRequest = restServer.takeRequest();
    assertEquals("POST", restRequest.getMethod());
    assertTrue(restRequest.getPath().startsWith("/projects?"));
    assertTrue(restRequest.getPath().contains("notifyMembers=true"));
    assertTrue(restRequest.getPath().contains("validateOnly=false"));
    assertTrue(restRequest.getPath().contains("lang=en"));
    assertEquals("application/json", restRequest.getHeader("Content-Type"));
    assertEquals("Bearer eyJhbnNpc2l0b3IuYm9sdXMubWFnbnVz", restRequest.getHeader("Authorization"));

    assertEquals(true, result.get("success"));
    Map<String, Object> data = (Map<String, Object>) result.get("data");
    assertEquals(55504, data.get("id"));
    assertEquals("CRM", data.get("name"));
    assertEquals("crm-20251111", data.get("code"));
    assertEquals(12345, data.get("ownerId"));
    assertEquals("2025-09-20T00:58:50.170784Z", data.get("created_at"));
    assertTrue(data.containsKey("members"));
    List<Integer> members = (List<Integer>) data.get("members");
    assertEquals(2, members.size());
    assertEquals(12345, members.get(0));
    assertEquals(67890, members.get(1));
  }

  @Test
  public void testOpenAPIBearerQueryInlinedBodyWithNegativeResponse() throws Exception {
    Workflow workflow =
        readWorkflowFromClasspath("workflows-samples/openapi/project-post-positive.yaml");

    URL url = this.getClass().getResource("/schema/openapi/openapi.yaml");

    Path workflowPath = Path.of(url.getPath());
    String yaml = Files.readString(workflowPath, StandardCharsets.UTF_8);

    openApiServer.enqueue(
        new MockResponse()
            .setBody(yaml)
            .setHeader("Content-Type", "application/yaml")
            .setResponseCode(200));

    restServer.enqueue(
        new MockResponse()
            .setBody(PROJECT_JSON_FALSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(409));

    Map<String, Object> result;

    Exception exception = null;

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      exception = e;
    }

    RecordedRequest restRequest = restServer.takeRequest();
    assertEquals("POST", restRequest.getMethod());
    assertTrue(restRequest.getPath().startsWith("/projects?"));
    assertTrue(restRequest.getPath().contains("notifyMembers=true"));
    assertTrue(restRequest.getPath().contains("validateOnly=false"));
    assertTrue(restRequest.getPath().contains("lang=en"));
    assertEquals("application/json", restRequest.getHeader("Content-Type"));
    assertEquals("Bearer eyJhbnNpc2l0b3IuYm9sdXMubWFnbnVz", restRequest.getHeader("Authorization"));

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("status=409"));
    assertTrue(exception.getMessage().contains("title=HTTP 409 Client Error"));
  }

  @Test
  public void testOpenAPIGetWithPositiveResponse() throws Exception {
    Workflow workflow =
        readWorkflowFromClasspath("workflows-samples/openapi/get-user-get-request.yaml");

    URL url = this.getClass().getResource("/schema/openapi/openapi.yaml");

    Path workflowPath = Path.of(url.getPath());
    String yaml = Files.readString(workflowPath, StandardCharsets.UTF_8);

    openApiServer.enqueue(
        new MockResponse()
            .setBody(yaml)
            .setHeader("Content-Type", "application/yaml")
            .setResponseCode(200));

    restServer.enqueue(
        new MockResponse()
            .setBody(PROJECT_GET_JSON_POSITIVE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    RecordedRequest restRequest = restServer.takeRequest();
    assertEquals("GET", restRequest.getMethod());
    assertTrue(restRequest.getPath().startsWith("/users/40099?"));

    assertTrue(result.containsKey("data"));
    Map<String, Object> data = (Map<String, Object>) result.get("data");
    assertEquals(40099, data.get("id"));
    assertEquals("Severus Calix", data.get("name"));
    assertEquals("severus.calix@hive-terra.example.com", data.get("email"));
  }

  @Test
  public void testOpenAPIGetWithPositiveResponseAndVars() throws Exception {
    Workflow workflow =
        readWorkflowFromClasspath("workflows-samples/openapi/get-user-get-request-vars.yaml");

    URL url = this.getClass().getResource("/schema/openapi/openapi.yaml");

    Path workflowPath = Path.of(url.getPath());
    String yaml = Files.readString(workflowPath, StandardCharsets.UTF_8);

    openApiServer.enqueue(
        new MockResponse()
            .setBody(yaml)
            .setHeader("Content-Type", "application/yaml")
            .setResponseCode(200));

    restServer.enqueue(
        new MockResponse()
            .setBody(PROJECT_GET_JSON_POSITIVE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Map<String, Object> result;
    Map<String, Object> params =
        Map.of(
            "userId",
            40099,
            "id",
            "id",
            "name",
            "name",
            "email",
            "email",
            "include_deleted",
            "false",
            "lang",
            "en",
            "format",
            "full",
            "limit",
            20);

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    RecordedRequest restRequest = restServer.takeRequest();
    assertEquals("GET", restRequest.getMethod());
    assertTrue(restRequest.getPath().startsWith("/users/40099?"));

    assertTrue(result.containsKey("data"));
    Map<String, Object> data = (Map<String, Object>) result.get("data");
    assertEquals(40099, data.get("id"));
    assertEquals("Severus Calix", data.get("name"));
    assertEquals("severus.calix@hive-terra.example.com", data.get("email"));
  }
}
