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
import io.serverlessworkflow.impl.auth.AuthProviderFactory;
import jakarta.ws.rs.HttpMethod;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class HttpExecutorBuilder {

  private final WorkflowDefinition definition;
  private WorkflowValueResolver<URI> pathSupplier;
  private Object body;
  private String method = HttpMethod.GET;
  private ReferenceableAuthenticationPolicy policy;
  private boolean redirect;
  private WorkflowValueResolver<Map<String, Object>> headersMap;
  private WorkflowValueResolver<Map<String, Object>> queryMap;

  private HttpExecutorBuilder(WorkflowDefinition definition) {
    this.definition = definition;
  }

  public HttpExecutorBuilder withAuth(ReferenceableAuthenticationPolicy policy) {
    this.policy = policy;
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

  public HttpExecutor build(String uri) {
    return build((w, f, n) -> URI.create(uri));
  }

  public HttpExecutor build(WorkflowValueResolver<URI> uriSupplier) {
    return new HttpExecutor(
        uriSupplier,
        Optional.ofNullable(headersMap),
        Optional.ofNullable(queryMap),
        buildRequestExecutor(),
        Optional.ofNullable(pathSupplier));
  }

  public static HttpExecutorBuilder builder(WorkflowDefinition definition) {
    return new HttpExecutorBuilder(definition);
  }

  private RequestExecutor buildRequestExecutor() {
    String theMethod = method.toUpperCase();
    switch (theMethod) {
      case HttpMethod.POST:
      case HttpMethod.PUT:
      case HttpMethod.PATCH:
        return new WithBodyRequestExecutor(
            theMethod,
            redirect,
            AuthProviderFactory.getAuth(definition, policy, method),
            definition.application(),
            body);
      case HttpMethod.DELETE:
      case HttpMethod.HEAD:
      case HttpMethod.OPTIONS:
      case HttpMethod.GET:
      default:
        return new WithoutBodyRequestExecutor(
            theMethod, redirect, AuthProviderFactory.getAuth(definition, policy, method));
    }
  }
}
