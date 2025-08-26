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

  private static final String RESPONSE =
      """
                  {
                      "message": "Hello World"
                  }
                  """;

  String TOKEN_RESPONSE_TEMPLATE =
      """
                  {
                    "access_token": "%s",
                    "token_type": "Bearer",
                    "expires_in": 3600,
                    "scope": "read write"
                  }
                  """;

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
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthClientSecretPostPasswordHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
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
  public void testOAuthClientSecretPostWithArgsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostPasswordAsArgHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
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
  public void testOAuthClientSecretPostWithArgsNoEndPointWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostPasswordNoEndpointsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
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
  public void testOAuthClientSecretPostWithArgsAllGrantsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostPasswordAllGrantsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test",
            "openidScope", "openidScope",
            "audience", "account");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
    assertEquals("application/x-www-form-urlencoded", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    assertTrue(tokenRequestBody.contains("grant_type=password"));
    assertTrue(tokenRequestBody.contains("username=serverless-workflow-test"));
    assertTrue(tokenRequestBody.contains("password=serverless-workflow-test"));

    assertTrue(
        tokenRequestBody.contains("scope=pets%3Aread+pets%3Awrite+pets%3Adelete+pets%3Acreate"));
    assertTrue(
        tokenRequestBody.contains("audience=serverless-workflow+another-audience+third-audience"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthClientSecretPostClientCredentialsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostClientCredentialsHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
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

  @Test
  public void testOAuthClientSecretPostClientCredentialsParamsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthClientSecretPostClientCredentialsParamsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
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

  @Test
  public void testOAuthClientSecretPostClientCredentialsParamsNoEndpointWorkflowExecution()
      throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath(
            "oAuthClientSecretPostClientCredentialsParamsNoEndPointHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
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

  @Test
  public void testOAuthJSONPasswordWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONPasswordHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());

    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();

    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(asJson.containsKey("grant_type") && asJson.get("grant_type").equals("password"));

    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));

    assertTrue(
        asJson.containsKey("username")
            && asJson.get("username").equals("serverless-workflow-test"));
    assertTrue(
        asJson.containsKey("password")
            && asJson.get("password").equals("serverless-workflow-test"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONWithArgsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONPasswordAsArgHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();

    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(asJson.containsKey("grant_type") && asJson.get("grant_type").equals("password"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));
    assertTrue(
        asJson.containsKey("username")
            && asJson.get("username").equals("serverless-workflow-test"));
    assertTrue(
        asJson.containsKey("password")
            && asJson.get("password").equals("serverless-workflow-test"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONWithArgsNoEndPointWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONPasswordNoEndpointsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(asJson.containsKey("grant_type") && asJson.get("grant_type").equals("password"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));
    assertTrue(
        asJson.containsKey("username")
            && asJson.get("username").equals("serverless-workflow-test"));
    assertTrue(
        asJson.containsKey("password")
            && asJson.get("password").equals("serverless-workflow-test"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONWithArgsAllGrantsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();
    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONPasswordAllGrantsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT",
            "username", "serverless-workflow-test",
            "password", "serverless-workflow-test",
            "openidScope", "openidScope",
            "audience", "account");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();

    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(asJson.containsKey("grant_type") && asJson.get("grant_type").equals("password"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));
    assertTrue(
        asJson.containsKey("username")
            && asJson.get("username").equals("serverless-workflow-test"));
    assertTrue(
        asJson.containsKey("password")
            && asJson.get("password").equals("serverless-workflow-test"));

    assertTrue(
        asJson.containsKey("scope")
            && asJson.get("scope").equals("pets:read pets:write pets:delete pets:create"));

    assertTrue(
        asJson.containsKey("audience")
            && asJson
                .get("audience")
                .equals("serverless-workflow another-audience third-audience"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONClientCredentialsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONClientCredentialsHttpCall.yaml");
    Map<String, Object> result;
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(Map.of()).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(
        asJson.containsKey("grant_type") && asJson.get("grant_type").equals("client_credentials"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONClientCredentialsParamsWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow = readWorkflowFromClasspath("oAuthJSONClientCredentialsParamsHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/protocol/openid-connect/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(
        asJson.containsKey("grant_type") && asJson.get("grant_type").equals("client_credentials"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));

    RecordedRequest petRequest = apiServer.takeRequest();
    assertEquals("GET", petRequest.getMethod());
    assertEquals("/hello", petRequest.getPath());
    assertEquals("Bearer " + jwt, petRequest.getHeader("Authorization"));
  }

  @Test
  public void testOAuthJSONClientCredentialsParamsNoEndpointWorkflowExecution() throws Exception {
    String jwt = fakeAccessToken();

    String tokenResponse = TOKEN_RESPONSE_TEMPLATE.formatted(jwt);

    authServer.enqueue(
        new MockResponse()
            .setBody(tokenResponse)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    apiServer.enqueue(
        new MockResponse()
            .setBody(RESPONSE)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Workflow workflow =
        readWorkflowFromClasspath("oAuthJSONClientCredentialsParamsNoEndPointHttpCall.yaml");
    Map<String, Object> result;
    Map<String, String> params =
        Map.of(
            "clientId", "serverless-workflow",
            "clientSecret", "D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT");

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      result =
          app.workflowDefinition(workflow).instance(params).start().get().asMap().orElseThrow();
    }

    assertTrue(result.containsKey("message"));
    assertTrue(result.get("message").toString().contains("Hello World"));

    RecordedRequest tokenRequest = authServer.takeRequest();
    assertEquals("POST", tokenRequest.getMethod());
    assertEquals("/realms/test-realm/oauth2/token", tokenRequest.getPath());
    assertEquals("application/json", tokenRequest.getHeader("Content-Type"));

    String tokenRequestBody = tokenRequest.getBody().readUtf8();
    Map<String, Object> asJson = MAPPER.readValue(tokenRequestBody, Map.class);
    assertTrue(
        asJson.containsKey("grant_type") && asJson.get("grant_type").equals("client_credentials"));
    assertTrue(
        asJson.containsKey("client_id") && asJson.get("client_id").equals("serverless-workflow"));
    assertTrue(
        asJson.containsKey("client_secret")
            && asJson.get("client_secret").equals("D0ACXCUKOUrL5YL7j6RQWplMaSjPB8MT"));

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
                "typ", "Bearer",
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
            "scope", "profile email",
            "exp", now + 3600,
            "iat", now));
  }
}
