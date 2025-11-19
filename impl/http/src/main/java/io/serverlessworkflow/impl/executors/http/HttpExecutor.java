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
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
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

  private WorkflowValueResolver<WebTarget> targetSupplier;
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
      return withHeaders(WorkflowUtils.buildMapResolver(definition.application(), headersMap));
    }

    public HttpExecutorBuilder withQueryMap(Map<String, Object> queryMap) {
      return withQueryMap(WorkflowUtils.buildMapResolver(definition.application(), queryMap));
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
      executor.requestFunction = buildRequestSupplier(method, body, definition.application());
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
            httpArgs.getMethod().toUpperCase(), httpArgs.getBody(), definition.application());
  }

  private static RequestSupplier buildRequestSupplier(
      String method, Object body, WorkflowApplication application) {

    switch (method.toUpperCase()) {
      case HttpMethod.POST:
        WorkflowFilter bodyFilter = WorkflowUtils.buildWorkflowFilter(application, body);
        return (request, w, t, node) -> {
          HttpModelConverter converter = HttpConverterResolver.converter(w, t);
          return w.definition()
              .application()
              .modelFactory()
              .fromAny(
                  request.post(
                      converter.toEntity(bodyFilter.apply(w, t, node)), converter.responseType()));
        };
      case HttpMethod.GET:
      default:
        return (request, w, t, n) ->
            w.definition()
                .application()
                .modelFactory()
                .fromAny(request.get(HttpConverterResolver.converter(w, t).responseType()));
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

  private static WorkflowValueResolver<WebTarget> getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier) {
    return (w, t, n) -> HttpClientResolver.client(w, t).target(uriSupplier.apply(w, t, n));
  }

  private static WorkflowValueResolver<WebTarget> getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier, WorkflowValueResolver<URI> pathSupplier) {
    return (w, t, n) -> {
      URI base = uriSupplier.apply(w, t, n);
      URI path = pathSupplier.apply(w, t, n);
      URI combined = buildTargetUri(base, path);
      return HttpClientResolver.client(w, t).target(combined);
    };
  }

  static URI buildTargetUri(URI base, URI path) {
    if (path.isAbsolute()) {
      return path;
    }

    String basePath = base.getPath();
    if (basePath == null || basePath.isEmpty()) {
      basePath = "/";
    } else if (!basePath.endsWith("/")) {
      basePath = basePath + "/";
    }

    String relPath = path.getPath() == null ? "" : path.getPath();
    while (relPath.startsWith("/")) {
      relPath = relPath.substring(1);
    }

    try {
      return new URI(
          base.getScheme(),
          base.getAuthority(),
          basePath + relPath,
          path.getQuery(),
          path.getFragment());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to build combined URI from base=" + base + " and path=" + path, e);
    }
  }
}
