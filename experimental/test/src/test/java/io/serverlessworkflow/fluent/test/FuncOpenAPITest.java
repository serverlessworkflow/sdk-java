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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.openapi;

import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.Headers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FuncOpenAPITest {

  private static MockWebServer mockWebServer;

  @BeforeEach
  public void setup() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start(0);
  }

  @AfterEach
  public void tearDown() {
    mockWebServer.close();
  }

  @Test
  void test_openapi_document_with_non_jq_uri_string() {
    String mockedSwaggerDoc =
        """
                {
                  "swagger": "2.0",
                  "info": { "version": "1.0.0", "title": "Mock Petstore" },
                  "host": "localhost:%d",
                  "basePath": "/v2",
                  "schemes": [ "http" ],
                  "paths": {
                    "/pet/findByStatus": {
                      "get": {
                        "operationId": "findPetsByStatus",
                        "parameters": [
                          {
                            "name": "status",
                            "in": "query",
                            "required": true,
                            "type": "string"
                          }
                        ],
                        "responses": { "200": { "description": "OK" } }
                      }
                    }
                  }
                }
                """
            .formatted(mockWebServer.getPort());

    mockWebServer.enqueue(
        new MockResponse(200, Headers.of("Content-Type", "application/json"), mockedSwaggerDoc));
    mockWebServer.enqueue(
        new MockResponse(
            200,
            Headers.of("Content-Type", "application/json"),
            """
                { "description": "OK" }
                """));
    var w =
        FuncWorkflowBuilder.workflow("openapi-call-workflow")
            .tasks(
                openapi()
                    .document(URI.create(mockWebServer.url("/v2/swagger.json").toString()))
                    .operation("findPetsByStatus")
                    .parameters(Map.of("status", "available")))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      WorkflowDefinition def = app.workflowDefinition(w);
      WorkflowInstance instance = def.instance(Map.of());
      WorkflowModel model = instance.start().join();

      SoftAssertions.assertSoftly(
          softly -> {
            softly.assertThat(model).isNotNull();
            softly.assertThat(model.asMap()).contains(Map.of("description", "OK"));
          });
    }
  }
}
