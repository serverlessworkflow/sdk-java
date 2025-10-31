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

import static io.serverlessworkflow.impl.WorkflowUtils.buildMapResolver;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HttpExecutor implements CallableTask<CallHTTP> {

  // TODO allow changing default converter
  private static final HttpModelConverter defaultConverter = new HttpModelConverter() {};

  private TargetSupplier targetSupplier;
  private Optional<WorkflowValueResolver<Map<String, Object>>> headersMap;
  private Optional<WorkflowValueResolver<Map<String, Object>>> queryMap;
  private Optional<AuthProvider> authProvider;
  private RequestSupplier requestFunction;

  public static class HttpExecutorBuilder {

    private final WorkflowDefinition definition;

    private ReferenceableAuthenticationPolicy authPolicy;
    private WorkflowValueResolver<Map<String, Object>> headersMap;
    private WorkflowValueResolver<Map<String, Object>> queryMap;
    private WorkflowValueResolver<URI> pathSupplier;
    private Object body;
    private boolean redirect;
    private String method = HttpMethod.GET;

    private HttpExecutorBuilder(WorkflowDefinition definition) {
      this.definition = definition;
    }

    public HttpExecutorBuilder withAuth(ReferenceableAuthenticationPolicy policy) {
      this.authPolicy = policy;
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

    public HttpExecutorBuilder withHeaders(WorkflowValueResolver<Map<String, Object>> headersMap) {
      this.headersMap = headersMap;
      return this;
    }

    public HttpExecutorBuilder withQueryMap(WorkflowValueResolver<Map<String, Object>> queryMap) {
      this.queryMap = queryMap;
      return this;
    }

    public HttpExecutorBuilder withHeaders(Map<String, Object> headersMap) {
      return withHeaders(
          definition
              .application()
              .expressionFactory()
              .resolveMap(ExpressionDescriptor.object(headersMap)));
    }

    public HttpExecutorBuilder withQueryMap(Map<String, Object> headersMap) {
      return withQueryMap(
          definition
              .application()
              .expressionFactory()
              .resolveMap(ExpressionDescriptor.object(headersMap)));
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
      HttpExecutor executor = new HttpExecutor();
      executor.targetSupplier =
          pathSupplier == null
              ? getTargetSupplier(uriSupplier)
              : getTargetSupplier(uriSupplier, pathSupplier);
      executor.authProvider = AuthProviderFactory.getAuth(definition, authPolicy);
      executor.requestFunction =
          buildRequestSupplier(method, body, definition.application(), defaultConverter);
      executor.headersMap = Optional.ofNullable(headersMap);
      executor.queryMap = Optional.ofNullable(queryMap);
      return executor;
    }
  }

  public static HttpExecutorBuilder builder(WorkflowDefinition definition) {
    return new HttpExecutorBuilder(definition);
  }

  @FunctionalInterface
  private interface RequestSupplier {
    WorkflowModel apply(
        Builder request, WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  @Override
  public void init(CallHTTP task, WorkflowDefinition definition) {
    final HTTPArguments httpArgs = task.getWith();
    final Endpoint endpoint = httpArgs.getEndpoint();

    this.authProvider =
        endpoint.getEndpointConfiguration() == null
            ? Optional.empty()
            : AuthProviderFactory.getAuth(
                definition, endpoint.getEndpointConfiguration().getAuthentication());

    this.targetSupplier = getTargetSupplier(definition.resourceLoader().uriSupplier(endpoint));
    this.headersMap =
        httpArgs.getHeaders() != null
            ? Optional.of(
                buildMapResolver(
                    definition.application(),
                    httpArgs.getHeaders().getRuntimeExpression(),
                    httpArgs.getHeaders().getHTTPHeaders() != null
                        ? httpArgs.getHeaders().getHTTPHeaders().getAdditionalProperties()
                        : null))
            : Optional.empty();
    this.queryMap =
        httpArgs.getQuery() != null
            ? Optional.of(
                buildMapResolver(
                    definition.application(),
                    httpArgs.getQuery().getRuntimeExpression(),
                    httpArgs.getQuery().getHTTPQuery() != null
                        ? httpArgs.getQuery().getHTTPQuery().getAdditionalProperties()
                        : null))
            : Optional.empty();
    this.requestFunction =
        buildRequestSupplier(
            httpArgs.getMethod().toUpperCase(),
            httpArgs.getBody(),
            definition.application(),
            defaultConverter);
  }

  private static RequestSupplier buildRequestSupplier(
      String method, Object body, WorkflowApplication application, HttpModelConverter converter) {
    switch (method.toUpperCase()) {
      case HttpMethod.POST:
        WorkflowValueResolver<Map<String, Object>> bodyFilter =
            buildMapResolver(application, null, body);
        return (request, w, context, node) ->
            converter.toModel(
                application.modelFactory(),
                node,
                request.post(
                    converter.toEntity(bodyFilter.apply(w, context, node)), node.objectClass()));
      case HttpMethod.GET:
      default:
        return (request, w, t, n) ->
            converter.toModel(application.modelFactory(), n, request.get(n.objectClass()));
    }
  }

  private static class TargetQuerySupplier implements Supplier<WebTarget> {

    private WebTarget target;

    public TargetQuerySupplier(WebTarget original) {
      this.target = original;
    }

    public void addQuery(String key, Object value) {
      target = target.queryParam(key, value);
    }

    public WebTarget get() {
      return target;
    }
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflow, TaskContext taskContext, WorkflowModel input) {
    TargetQuerySupplier supplier =
        new TargetQuerySupplier(targetSupplier.apply(workflow, taskContext, input));
    queryMap.ifPresent(
        q -> q.apply(workflow, taskContext, input).forEach((k, v) -> supplier.addQuery(k, v)));
    Builder request = supplier.get().request();
    headersMap.ifPresent(
        h -> h.apply(workflow, taskContext, input).forEach((k, v) -> request.header(k, v)));
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            authProvider.ifPresent(auth -> auth.build(request, workflow, taskContext, input));
            return requestFunction.apply(request, workflow, taskContext, input);
          } catch (WebApplicationException exception) {
            throw new WorkflowException(
                WorkflowError.communication(
                        exception.getResponse().getStatus(), taskContext, exception)
                    .build());
          }
        },
        workflow.definition().application().executorService());
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallHTTP.class);
  }

  private static TargetSupplier getTargetSupplier(WorkflowValueResolver<URI> uriSupplier) {
    return (w, t, n) -> HttpClientResolver.client(w, t).target(uriSupplier.apply(w, t, n));
  }

  private static TargetSupplier getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier, WorkflowValueResolver<URI> pathSupplier) {
    return (w, t, n) ->
        HttpClientResolver.client(w, t)
            .target(uriSupplier.apply(w, t, n).resolve(pathSupplier.apply(w, t, n)));
  }

  private static interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }
}
