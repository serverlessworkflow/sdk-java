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
package io.serverlessworkflow.impl.executors.http.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.auth.AccessTokenProvider;
import io.serverlessworkflow.impl.auth.HttpRequestInfo;
import io.serverlessworkflow.impl.auth.JWTConverter;
import io.serverlessworkflow.impl.auth.TokenIntrospection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JaxRSAccessTokenProviderTest {

  private MockWebServer server;
  private WorkflowContext workflow;
  private TaskContext task;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    workflow = mock(WorkflowContext.class, RETURNS_DEEP_STUBS);
    when(workflow.definition().application().additionalObject(any(), any(), any()))
        .thenReturn(Optional.empty());
    task = mock(TaskContext.class, RETURNS_DEEP_STUBS);
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void introspectSendsClientAuthAndTokenAndParsesActive() throws Exception {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"active\": true, \"scope\": \"read\"}"));

    TokenIntrospection result =
        provider().introspect(workflow, task, null, "the-access-token", "access_token");

    assertTrue(result.active());
    assertEquals("read", result.claims().get("scope"));

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/oauth2/introspect", request.getPath());
    assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("token=the-access-token"));
    assertTrue(body.contains("token_type_hint=access_token"));
    assertTrue(body.contains("client_id=serverless-workflow"));
    assertTrue(body.contains("client_secret=top-secret"));
  }

  @Test
  void introspectReturnsInactiveForInactiveToken() throws Exception {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"active\": false}"));

    TokenIntrospection result = provider().introspect(workflow, task, null, "stale-token", null);

    assertFalse(result.active());
    RecordedRequest request = server.takeRequest();
    assertFalse(request.getBody().readUtf8().contains("token_type_hint"));
  }

  @Test
  void revokeSendsClientAuthAndToken() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    provider().revoke(workflow, task, null, "the-access-token", "access_token");

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/oauth2/revoke", request.getPath());
    assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("token=the-access-token"));
    assertTrue(body.contains("token_type_hint=access_token"));
    assertTrue(body.contains("client_id=serverless-workflow"));
  }

  @Test
  void revokeRaisesWorkflowExceptionOnErrorResponse() {
    server.enqueue(new MockResponse().setResponseCode(400).setBody("unsupported_token_type"));

    assertThrows(
        WorkflowException.class, () -> provider().revoke(workflow, task, null, "bad-token", null));
  }

  private AccessTokenProvider provider() {
    JWTConverter converter = token -> null;
    HttpRequestInfo requestInfo =
        new HttpRequestInfo(
            Map.of(),
            Map.of(),
            Map.of(
                "client_id", value("serverless-workflow"),
                "client_secret", value("top-secret")),
            value(server.url("/oauth2/token").uri()),
            Optional.of(value(server.url("/oauth2/revoke").uri())),
            Optional.of(value(server.url("/oauth2/introspect").uri())),
            "client_credentials",
            "application/x-www-form-urlencoded");
    return new JaxRSAccessTokenProviderFactory().build(requestInfo, List.of(), converter);
  }

  private static <T> WorkflowValueResolver<T> value(T value) {
    return (w, t, m) -> value;
  }
}
