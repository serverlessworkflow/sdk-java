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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.auth.AccessTokenProvider;
import io.serverlessworkflow.impl.auth.HttpRequestInfo;
import io.serverlessworkflow.impl.auth.JWTConverter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
