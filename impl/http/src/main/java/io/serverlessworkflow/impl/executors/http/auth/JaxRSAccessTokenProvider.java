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

import static io.serverlessworkflow.api.types.OAuth2TokenRequest.Oauth2TokenRequestEncoding.APPLICATION_X_WWW_FORM_URLENCODED;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.auth.AccessTokenProvider;
import io.serverlessworkflow.impl.auth.HttpRequestInfo;
import io.serverlessworkflow.impl.auth.JWT;
import io.serverlessworkflow.impl.auth.JWTConverter;
import io.serverlessworkflow.impl.auth.TokenIntrospection;
import io.serverlessworkflow.impl.executors.http.HttpClientResolver;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

class JaxRSAccessTokenProvider implements AccessTokenProvider {

  private final List<String> issuers;
  private final HttpRequestInfo requestInfo;
  private final JWTConverter jwtConverter;

  JaxRSAccessTokenProvider(
      HttpRequestInfo requestInfo, List<String> issuers, JWTConverter converter) {
    this.requestInfo = requestInfo;
    this.issuers = issuers;
    this.jwtConverter = converter;
  }

  public JWT validateAndGet(WorkflowContext workflow, TaskContext context, WorkflowModel model) {
    Map<String, Object> token = invoke(workflow, context, model);
    JWT jwt = jwtConverter.fromToken((String) token.get("access_token"));
    if (issuers != null && !issuers.isEmpty()) {
      jwt.issuer()
          .ifPresent(
              issuer -> {
                if (!issuers.contains(issuer)) {
                  throw new IllegalStateException("Token issuer is not valid: " + issuer);
                }
              });
    }
    return jwt;
  }

  @Override
  public TokenIntrospection introspect(
      WorkflowContext workflow,
      TaskContext context,
      WorkflowModel model,
      String token,
      String tokenTypeHint) {
    URI uri = endpoint(requestInfo.introspectionUri(), "introspection", workflow, context, model);
    return execute(
        context,
        () -> {
          Response response =
              postManagementRequest(workflow, context, model, uri, token, tokenTypeHint);
          ensureSuccessful(response, context, "introspect token");
          Map<String, Object> body = response.readEntity(new GenericType<>() {});
          return new TokenIntrospection(Boolean.TRUE.equals(body.get("active")), body);
        });
  }

  @Override
  public void revoke(
      WorkflowContext workflow,
      TaskContext context,
      WorkflowModel model,
      String token,
      String tokenTypeHint) {
    URI uri = endpoint(requestInfo.revocationUri(), "revocation", workflow, context, model);
    execute(
        context,
        () -> {
          // RFC 7009: a successful revocation responds with HTTP 200 and an empty body.
          Response response =
              postManagementRequest(workflow, context, model, uri, token, tokenTypeHint);
          ensureSuccessful(response, context, "revoke token");
          return null;
        });
  }

  private Map<String, Object> invoke(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel model) {
    return execute(
        taskContext,
        () -> {
          Response response = executeRequest(workflowContext, taskContext, model);
          ensureSuccessful(response, taskContext, "obtain token");
          return response.readEntity(new GenericType<>() {});
        });
  }

  private Response executeRequest(WorkflowContext workflow, TaskContext task, WorkflowModel model) {

    Client client = HttpClientResolver.client(workflow, task);
    WebTarget target = client.target(requestInfo.uri().apply(workflow, task, model));

    Invocation.Builder builder = commonHeaders(target, workflow, task, model);
    builder.header("grant_type", requestInfo.grantType());

    Entity<?> entity;
    if (requestInfo.contentType().equals(APPLICATION_X_WWW_FORM_URLENCODED.value())) {
      Form form = new Form();
      form.param("grant_type", requestInfo.grantType());
      requestInfo
          .queryParams()
          .forEach((key, value) -> form.param(key, value.apply(workflow, task, model)));
      requestInfo
          .clientAuthParams()
          .forEach((key, value) -> form.param(key, value.apply(workflow, task, model)));
      entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED);
    } else {
      Map<String, Object> jsonData = new HashMap<>();
      jsonData.put("grant_type", requestInfo.grantType());
      requestInfo
          .queryParams()
          .forEach((key, value) -> jsonData.put(key, value.apply(workflow, task, model)));
      requestInfo
          .clientAuthParams()
          .forEach((key, value) -> jsonData.put(key, value.apply(workflow, task, model)));
      entity = Entity.entity(jsonData, MediaType.APPLICATION_JSON);
    }

    return builder.post(entity);
  }

  /**
   * Builds and posts a token management request (revocation per RFC 7009, introspection per RFC
   * 7662). The body carries the {@code token}, an optional {@code token_type_hint} and the client
   * authentication parameters; client authentication carried through headers (e.g. HTTP Basic) is
   * applied as well.
   */
  private Response postManagementRequest(
      WorkflowContext workflow,
      TaskContext task,
      WorkflowModel model,
      URI uri,
      String token,
      String tokenTypeHint) {
    Invocation.Builder builder =
        commonHeaders(HttpClientResolver.client(workflow, task).target(uri), workflow, task, model);

    Form form = new Form();
    form.param("token", token);
    if (tokenTypeHint != null) {
      form.param("token_type_hint", tokenTypeHint);
    }
    requestInfo
        .clientAuthParams()
        .forEach((key, value) -> form.param(key, value.apply(workflow, task, model)));

    return builder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
  }

  private Invocation.Builder commonHeaders(
      WebTarget target, WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
    builder.header("User-Agent", "OAuth2-Client-Credentials/1.0");
    builder.header("Accept", MediaType.APPLICATION_JSON);
    builder.header("Cache-Control", "no-cache");
    for (var entry : requestInfo.headers().entrySet()) {
      String headerValue = entry.getValue().apply(workflow, task, model);
      if (headerValue != null) {
        builder.header(entry.getKey(), headerValue);
      }
    }
    return builder;
  }

  private URI endpoint(
      Optional<WorkflowValueResolver<URI>> resolver,
      String name,
      WorkflowContext workflow,
      TaskContext task,
      WorkflowModel model) {
    return resolver
        .map(r -> r.apply(workflow, task, model))
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "No " + name + " endpoint is configured for this provider"));
  }

  private void ensureSuccessful(Response response, TaskContext task, String action) {
    int status = response.getStatus();
    if (status < 200 || status >= 300) {
      throw new WorkflowException(
          WorkflowError.communication(
                  status,
                  task,
                  "Failed to " + action + ": HTTP " + status + " — " + readError(response))
              .build());
    }
  }

  private static String readError(Response response) {
    return response.hasEntity() ? response.readEntity(String.class) : "";
  }

  private <T> T execute(TaskContext task, Supplier<T> call) {
    try {
      return call.get();
    } catch (ResponseProcessingException e) {
      throw new WorkflowException(
          WorkflowError.communication(
                  e.getResponse().getStatus(),
                  task,
                  "Failed to process response: " + e.getMessage())
              .build(),
          e);
    } catch (ProcessingException e) {
      throw new WorkflowException(
          WorkflowError.communication(task, "Failed to connect or process request: " + e.getMessage())
              .build(),
          e);
    }
  }
}
