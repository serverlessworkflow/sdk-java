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

import io.serverlessworkflow.api.types.EndpointConfiguration;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.auth.AuthProvider;
import io.serverlessworkflow.impl.auth.AuthProviderFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomAuthProviderFactoryOverrideTest {

  private static final String RESPONSE =
      """
          {
              "message": "Hello World"
          }
          """;

  private MockWebServer authServer;
  private MockWebServer apiServer;

  @BeforeEach
  void setUp() throws IOException {
    authServer = new MockWebServer();
    authServer.start(8888);

    apiServer = new MockWebServer();
    apiServer.start(8081);
  }

  @AfterEach
  void tearDown() throws IOException {
    authServer.shutdown();
    apiServer.shutdown();
  }

  @Test
  public void frameworkOverrideSuppliesAuthHeaderWithoutTokenExchange() throws Exception {
    final String frameworkToken = "framework-managed-token";
    final AtomicInteger factoryInvocations = new AtomicInteger();

    AuthProviderFactory frameworkFactory =
        new AuthProviderFactory() {
          @Override
          public Optional<AuthProvider> getAuth(
              WorkflowDefinition definition, EndpointConfiguration configuration) {
            return getAuth(
                definition,
                configuration == null ? null : configuration.getAuthentication(),
                "GET");
          }

          @Override
          public Optional<AuthProvider> getAuth(
              WorkflowDefinition definition,
              ReferenceableAuthenticationPolicy auth,
              String method) {
            factoryInvocations.incrementAndGet();
            return Optional.of(
                new AuthProvider() {
                  @Override
                  public String scheme() {
                    return "Bearer";
                  }

                  @Override
                  public String content(
                      WorkflowContext workflow, TaskContext task, WorkflowModel model, URI uri) {
                    return frameworkToken;
                  }
                });
          }
        };

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    try (WorkflowApplication app =
        WorkflowApplication.builder().withAuthProviderFactory(frameworkFactory).build()) {

      Workflow workflow =
          readWorkflowFromClasspath(
              "workflows-samples/oauth2/oAuthClientSecretPostClientCredentialsHttpCall.yaml");
      WorkflowDefinition definition = app.workflowDefinition(workflow);

      // Run twice on the same definition to check if the AuthProvider is resolved once at build
      // time
      for (int i = 0; i < 2; i++) {
        Map<String, Object> result =
            definition.instance(Map.of()).start().get().asMap().orElseThrow();
        assertTrue(result.get("message").toString().contains("Hello World"));
      }
    }

    for (int i = 0; i < 2; i++) {
      RecordedRequest apiRequest = apiServer.takeRequest();
      assertEquals("GET", apiRequest.getMethod());
      assertEquals("/hello", apiRequest.getPath());
      assertEquals("Bearer " + frameworkToken, apiRequest.getHeader("Authorization"));
    }

    // The SDK never performed a token exchange
    assertEquals(0, authServer.getRequestCount());

    assertEquals(1, factoryInvocations.get());
  }
}
