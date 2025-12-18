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
package io.serverlessworkflow.impl.executors.http;

import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.auth.AuthProvider;
import io.serverlessworkflow.impl.auth.AuthProviderFactory;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class HttpExecutorBuilder {

  private final WorkflowDefinition definition;
  private WorkflowValueResolver<URI> pathSupplier;
  private Object body;
  private String method = HttpMethod.GET;
  private boolean redirect;
  private Optional<WorkflowValueResolver<Duration>> timeout = Optional.empty();
  private WorkflowValueResolver<Map<String, Object>> headersMap;
  private WorkflowValueResolver<Map<String, Object>> queryMap;
  private Optional<AuthProvider> authProvider = Optional.empty();

  private HttpExecutorBuilder(WorkflowDefinition definition) {
    this.definition = definition;
  }

  public HttpExecutorBuilder withAuth(ReferenceableAuthenticationPolicy policy) {
    this.authProvider = AuthProviderFactory.getAuth(definition, policy);
    return this;
  }

  public HttpExecutorBuilder withBody(Object body) {
    this.body = body;
    return this;
  }

  public HttpExecutorBuilder withPath(WorkflowValueResolver<URI> pathSupplier) {
    this.pathSupplier = pathSupplier;
    return this;
  }

  public HttpExecutorBuilder withHeaders(Map<String, Object> headersMap) {
    return withHeaders(WorkflowUtils.buildMapResolver(definition.application(), headersMap));
  }

  public HttpExecutorBuilder withQueryMap(Map<String, Object> queryMap) {
    return withQueryMap(WorkflowUtils.buildMapResolver(definition.application(), queryMap));
  }

  public HttpExecutorBuilder withHeaders(WorkflowValueResolver<Map<String, Object>> headersMap) {
    this.headersMap = headersMap;
    return this;
  }

  public HttpExecutorBuilder withQueryMap(WorkflowValueResolver<Map<String, Object>> queryMap) {
    this.queryMap = queryMap;
    return this;
  }

  public HttpExecutorBuilder withMethod(String method) {
    this.method = method;
    return this;
  }

  public HttpExecutorBuilder redirect(boolean redirect) {
    this.redirect = redirect;
    return this;
  }

  public HttpExecutorBuilder timeout(Optional<WorkflowValueResolver<Duration>> timeout) {
    this.timeout = timeout;
    return this;
  }

  public HttpExecutor build(String uri) {
    return build((w, f, n) -> URI.create(uri));
  }

  public HttpExecutor build(WorkflowValueResolver<URI> uriSupplier) {

    return new HttpExecutor(
        getTargetSupplier(uriSupplier),
        Optional.ofNullable(headersMap),
        Optional.ofNullable(queryMap),
        authProvider,
        buildRequestSupplier());
  }

  private WorkflowValueResolver<WebTarget> getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier) {
    return pathSupplier == null
        ? (w, t, n) ->
            HttpClientResolver.client(w, t, redirect, timeout.map(v -> v.apply(w, t, n)))
                .target(uriSupplier.apply(w, t, n))
        : (w, t, n) ->
            HttpClientResolver.client(w, t, redirect, timeout.map(v -> v.apply(w, t, n)))
                .target(
                    WorkflowUtils.concatURI(
                        uriSupplier.apply(w, t, n), pathSupplier.apply(w, t, n)));
  }

  public static HttpExecutorBuilder builder(WorkflowDefinition definition) {
    return new HttpExecutorBuilder(definition);
  }

  private RequestSupplier buildRequestSupplier() {
    switch (method.toUpperCase()) {
      case HttpMethod.POST:
        return new WithBodyRequestSupplier(
            Invocation.Builder::post, definition.application(), body, redirect);
      case HttpMethod.PUT:
        return new WithBodyRequestSupplier(
            Invocation.Builder::put, definition.application(), body, redirect);
      case HttpMethod.DELETE:
        return new WithoutBodyRequestSupplier(
            Invocation.Builder::delete, definition.application(), redirect);
      case HttpMethod.HEAD:
        return new WithoutBodyRequestSupplier(
            Invocation.Builder::head, definition.application(), redirect);
      case HttpMethod.PATCH:
        return new WithBodyRequestSupplier(
            (request, entity) -> request.method("PATCH", entity),
            definition.application(),
            body,
            redirect);
      case HttpMethod.OPTIONS:
        return new WithoutBodyRequestSupplier(
            Invocation.Builder::options, definition.application(), redirect);
      case HttpMethod.GET:
      default:
        return new WithoutBodyRequestSupplier(
            Invocation.Builder::get, definition.application(), redirect);
    }
  }
}
