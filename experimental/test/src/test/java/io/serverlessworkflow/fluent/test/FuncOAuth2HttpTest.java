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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FuncOAuth2HttpTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private record OAuth2Client(String clientId, String clientSecret, String baseUrl) {}

  private WorkflowApplication app;
  private MockWebServer mockServer;

  // Recorded token-request bodies, keyed by the request path.
  private final Map<String, String> tokenRequestBodies = new ConcurrentHashMap<>();
  // Recorded Authorization headers of the downstream API calls, keyed by the request path.
  private final Map<String, String> apiAuthHeaders = new ConcurrentHashMap<>();

  @BeforeEach
  void setup() throws IOException {
    app = WorkflowApplication.builder().build();
    mockServer = new MockWebServer();
    mockServer.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            String path = request.getUrl().encodedPath();
            if (path.endsWith("/token")) {
              tokenRequestBodies.put(path, request.getBody().utf8());
              return new MockResponse(
                  200, Headers.of("Content-Type", "application/json"), tokenResponse(fakeJwt()));
            }
            // The downstream API call ("/joogle" or "/jahoo").
            apiAuthHeaders.put(path, request.getHeaders().get("Authorization"));
            return new MockResponse(
                200,
                Headers.of("Content-Type", "application/json"),
                "{\"email\":\"" + path + "@example.com\"}");
          }
        });
    mockServer.start(0);
  }

  @AfterEach
  void cleanup() {
    mockServer.close();
    app.close();
  }

  @Test
  @DisplayName(
      "Two named OAuth2 client-credentials authentications, each used by a forked HTTP call")
  void test_multiple_oauth2_clients() throws Exception {
    String base = mockServer.url("/").toString();
    base = base.substring(0, base.length() - 1); // strip trailing slash

    OAuth2Client joogle =
        new OAuth2Client("joogle-client-id", "joogle-client-secret", base + "/joogle-auth");
    OAuth2Client jahoo =
        new OAuth2Client("jahoo-client-id", "jahoo-client-secret", base + "/jahoo-auth");
    String wireMock = base;

    Workflow workflow =
        FuncWorkflowBuilder.workflow("multiple-oauth2-clients", "quarkus-flow")
            .use(
                use ->
                    use.authentications(
                        auth -> {
                          auth.authentication(
                              "joogle",
                              a ->
                                  a.oauth2(
                                      oauth2 ->
                                          oauth2
                                              .client(
                                                  client ->
                                                      client
                                                          .id(joogle.clientId())
                                                          .secret(joogle.clientSecret())
                                                          .authentication(
                                                              OAuth2AuthenticationDataClient
                                                                  .ClientAuthentication
                                                                  .CLIENT_SECRET_POST))
                                              .authority(joogle.baseUrl())
                                              .grant(
                                                  OAuth2AuthenticationData
                                                      .OAuth2AuthenticationDataGrant
                                                      .CLIENT_CREDENTIALS)
                                              .build()));
                          auth.authentication(
                              "jahoo",
                              a ->
                                  a.oauth2(
                                      oauth2 ->
                                          oauth2
                                              .client(
                                                  client ->
                                                      client
                                                          .id(jahoo.clientId())
                                                          .secret(jahoo.clientSecret()))
                                              .authority(jahoo.baseUrl())
                                              .grant(
                                                  OAuth2AuthenticationData
                                                      .OAuth2AuthenticationDataGrant
                                                      .CLIENT_CREDENTIALS)
                                              .build()));
                        }))
            .tasks(
                FuncDSL.fork(
                    FuncDSL.http()
                        .GET()
                        .uri(URI.create(wireMock + "/joogle"), FuncDSL.use("joogle")),
                    FuncDSL.http()
                        .GET()
                        .uri(URI.create(wireMock + "/jahoo"), FuncDSL.use("jahoo"))),
                FuncDSL.function("merge", o -> o))
            .build();

    app.workflowDefinition(workflow).instance(Map.of()).start().join();

    String joogleTokenBody = tokenRequestBodies.get("/joogle-auth/oauth2/token");
    String jahooTokenBody = tokenRequestBodies.get("/jahoo-auth/oauth2/token");

    assertThat(joogleTokenBody)
        .as("joogle token request body")
        .isNotNull()
        .contains("grant_type=client_credentials")
        .contains("client_id=joogle-client-id")
        .contains("client_secret=joogle-client-secret");

    assertThat(jahooTokenBody)
        .as("jahoo token request body")
        .isNotNull()
        .contains("grant_type=client_credentials")
        .contains("client_id=jahoo-client-id")
        .contains("client_secret=jahoo-client-secret");

    // The token obtained from each authority is forwarded as a Bearer token on the API call.
    assertThat(apiAuthHeaders.get("/joogle")).as("joogle bearer").startsWith("Bearer ");
    assertThat(apiAuthHeaders.get("/jahoo")).as("jahoo bearer").startsWith("Bearer ");
  }

  @Test
  @DisplayName("Custom endpoints.token overrides the default /oauth2/token path")
  void test_custom_token_endpoint() throws Exception {
    String base = mockServer.url("/").toString();
    base = base.substring(0, base.length() - 1); // strip trailing slash

    OAuth2Client joogle =
        new OAuth2Client("joogle-client-id", "joogle-client-secret", base + "/joogle-auth");
    String wireMock = base;
    String customTokenPath = "/auth/realms/joogle/protocol/openid-connect/token";

    // Same configuration as above, expressed with the FuncDSL auth helpers:
    // FuncDSL.auth(name, configurer) returns a chainable UseSpec (a Consumer<UseBuilder>),
    // and FuncDSL.oauth2(...) builds the policy (no explicit .build() needed).
    Workflow workflow =
        FuncWorkflowBuilder.workflow("custom-token-endpoint", "quarkus-flow")
            .use(
                FuncDSL.auth(
                    "joogle",
                    FuncDSL.oauth2(
                        oauth2 ->
                            oauth2
                                .endpoints(e -> e.token(customTokenPath))
                                .client(
                                    client ->
                                        client
                                            .id(joogle.clientId())
                                            .secret(joogle.clientSecret())
                                            .authentication(
                                                OAuth2AuthenticationDataClient.ClientAuthentication
                                                    .CLIENT_SECRET_POST))
                                .authority(joogle.baseUrl())
                                .grant(
                                    OAuth2AuthenticationData.OAuth2AuthenticationDataGrant
                                        .CLIENT_CREDENTIALS))))
            .tasks(
                FuncDSL.http().GET().uri(URI.create(wireMock + "/joogle"), FuncDSL.use("joogle")))
            .build();

    app.workflowDefinition(workflow).instance(Map.of()).start().join();

    // The token path is resolved relative to the authority, so the default "/oauth2/token" is
    // replaced by the custom path.
    assertThat(tokenRequestBodies.get("/joogle-auth" + customTokenPath))
        .as("token request hit the custom endpoint")
        .isNotNull()
        .contains("client_id=joogle-client-id");
    assertThat(tokenRequestBodies).doesNotContainKey("/joogle-auth/oauth2/token");
  }

  private static String tokenResponse(String jwt) {
    return """
        {
          "access_token": "%s",
          "token_type": "Bearer",
          "expires_in": 3600
        }
        """
        .formatted(jwt);
  }

  private static String fakeJwt() {
    try {
      long now = Instant.now().getEpochSecond();
      String header =
          MAPPER.writeValueAsString(Map.of("alg", "RS256", "typ", "Bearer", "kid", "test"));
      String payload =
          MAPPER.writeValueAsString(Map.of("sub", "test-subject", "exp", now + 3600, "iat", now));
      return b64Url(header) + "." + b64Url(payload) + ".sig";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String b64Url(String s) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }
}
