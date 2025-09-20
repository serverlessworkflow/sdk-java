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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public void testOpenAPIBearerQueryInlinedBodyWithPositiveResponce() throws Exception {
    Workflow workflow =
        readWorkflowFromClasspath("workflows-samples/openapi/project-post-positive.yaml");

    URL url = this.getClass().getResource("/workflows-samples/openapi/schema.yaml");

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
            .setResponseCode(200));

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
  }
}
