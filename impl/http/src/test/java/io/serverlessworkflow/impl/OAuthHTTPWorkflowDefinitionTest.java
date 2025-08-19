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

package io.serverlessworkflow.impl;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.types.Workflow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OAuthHTTPWorkflowDefinitionTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer authServer;
  private MockWebServer apiServer;
  private OkHttpClient httpClient;
  private String authBaseUrl;
  private String apiBaseUrl;

  @BeforeEach
  void setUp() throws IOException {
    authServer = new MockWebServer();
    authServer.start(8888);
    authBaseUrl = "http://localhost:8888";

    apiServer = new MockWebServer();
    apiServer.start(8081);
    apiBaseUrl = "http://localhost:8081";

    httpClient = new OkHttpClient();
  }

  @AfterEach
  void tearDown() throws IOException {
    authServer.shutdown();
    apiServer.shutdown();
  }

  @Test
  public void testOAuthClientSecretPostPasswordWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse =
        """
          {
            "access_token": "%s",
            "token_type": "Bearer",
            "expires_in": 3600,
            "scope": "read write"
          }
          """
            .formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    String response =
        """
            {
                "message": "Hello World"
            }
            """;

    apiServer.enqueue(
        new MockResponse()
            .setBody(response)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthClientSecretPostPasswordHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/x-www-form-urlencoded", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    assertTrue(tokenRequestBody.contains("grant_type=password"));
    assertTrue(tokenRequestBody.contains("username=serverless-workflow-test"));
    assertTrue(tokenRequestBody.contains("password=serverless-workflow-test"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthClientSecretPostClientCredentialsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse =
        """
              {
                "access_token": "%s",
                "token_type": "Bearer",
                "expires_in": 3600,
                "scope": "read write"
              }
              """
            .formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    String response =
        """
                {
                    "message": "Hello World"
                }
                """;

    apiServer.enqueue(
        new MockResponse()
            .setBody(response)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostClientCredentialsHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    } catch (Exception e) {
      throw new RuntimeException("Workflow execution failed", e);
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/x-www-form-urlencoded", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    assertTrue(tokenRequestBody.contains("grant_type=client_credentials"));
    assertTrue(tokenRequestBody.contains("client_id=serverless-workflow"));
    assertTrue(tokenRequestBody.contains("secret=D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  public static String fakeJwt(Map<String, Object> payload) throws Exception {
    String headerJson =
        MAPPER.writeValueAsString(
            Map.of(
                "alg", "RS256",
                "typ", "JWT",
                "kid", "test"));
    String payloadJson = MAPPER.writeValueAsString(payload);
    return b64Url(headerJson) + "." + b64Url(payloadJson) + ".sig";
  }

  private static String b64Url(String s) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }

  public static String fakeAccessToken() throws Exception {
    long now = Instant.now().getEpochSecond();
    return fakeJwt(
        Map.of(
            "iss", "http://localhost:8888/realms/test-realm",
            "aud", "account",
            "sub", "test-subject",
            "azp", "serverless-workflow",
            "typ", "Bearer",
            "scope", "profile email",
            "exp", now + 3600,
            "iat", now));
  }
}
