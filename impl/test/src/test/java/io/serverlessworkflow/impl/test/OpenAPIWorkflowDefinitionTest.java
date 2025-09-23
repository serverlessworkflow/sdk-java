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

import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class OpenAPIWorkflowDefinitionTest {

  private static WorkflowApplication app;
  private MockWebServer mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start(9999);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockServer.shutdown();
  }

  @BeforeAll
  static void setUpApp() {
    app = WorkflowApplication.builder().build();
  }

  @Test
  void testOpenAPIWorkflowExecution() throws IOException {

    WorkflowModel model =
        app.workflowDefinition(
                WorkflowReader.readWorkflowFromClasspath(
                    "workflows-samples/openapi/findPetsByStatus.yaml"))
            .instance(List.of())
            .start()
            .join();

    Assertions.assertThat(model.asCollection()).isNotEmpty();
    Assertions.assertThat(model.asCollection())
        .allMatch(
            m -> {
              Map<String, Object> pet = m.asMap().orElseThrow(RuntimeException::new);
              return pet.get("status").equals("available");
            });
  }

  @Test
  @DisplayName(
      "must raise an error for response status codes outside the 200–299 range when redirect is set to false")
  void testOpenAPIRedirect() {
    mockServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                                        {
                                          "openapi": "3.0.3",
                                          "info": {
                                            "title": "Redirect API",
                                            "version": "1.0.0"
                                          },
                                          "servers": [
                                            {
                                              "url": "http://localhost:9999"
                                            }
                                          ],
                                          "paths": {
                                            "/redirect": {
                                              "get": {
                                                "operationId": "redirectToDocs",
                                                "summary": "Redirects to external documentation",
                                                "responses": {
                                                  "302": {
                                                    "description": "Redirecting to external documentation"
                                                    }
                                                  }
                                                }
                                              }
                                            }
                                          }
                                        }
                                        """)
            .setHeader("Content-Type", "application/json"));

    mockServer.enqueue(new MockResponse().setResponseCode(301));

    Assertions.assertThatThrownBy(
            () ->
                app.workflowDefinition(
                        WorkflowReader.readWorkflowFromClasspath(
                            "workflows-samples/openapi/findPetsByStatus-redirect.yaml"))
                    .instance(List.of())
                    .start()
                    .join())
        .hasCauseInstanceOf(WorkflowException.class);
  }
}
