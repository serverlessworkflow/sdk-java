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
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Map;

public class HttpExecutorBuilder extends AbstractHttpExecutorBuilder {

  private final WorkflowDefinition definition;
  private WorkflowValueResolver<URI> pathSupplier;
  private Object body;
  private String method = HttpMethod.GET;
  private boolean redirect;

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

  public HttpExecutor build(String uri) {
    return build((w, f, n) -> URI.create(uri));
  }

  public HttpExecutor build(WorkflowValueResolver<URI> uriSupplier) {
    this.requestFunction = buildRequestSupplier(method, body, redirect, definition.application());
    this.targetSupplier =
        pathSupplier == null
            ? getTargetSupplier(uriSupplier)
            : getTargetSupplier(uriSupplier, pathSupplier);
    return build();
  }

  private static WorkflowValueResolver<WebTarget> getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier, WorkflowValueResolver<URI> pathSupplier) {
    return (w, t, n) ->
        HttpClientResolver.client(w, t)
            .target(
                WorkflowUtils.concatURI(uriSupplier.apply(w, t, n), pathSupplier.apply(w, t, n)));
  }

  public static HttpExecutorBuilder builder(WorkflowDefinition definition) {
    return new HttpExecutorBuilder(definition);
  }
}
