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
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HttpExecutor implements CallableTask<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private TargetSupplier targetSupplier;
  private Optional<WorkflowValueResolver<Map<String, Object>>> headersMap;
  private Optional<WorkflowValueResolver<Map<String, Object>>> queryMap;
  private Optional<AuthProvider> authProvider;
  private RequestSupplier requestFunction;
  private HttpModelConverter converter = new HttpModelConverter() {};

  @FunctionalInterface
  private interface RequestSupplier {
    WorkflowModel apply(
        Builder request, WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  @Override
  public void init(CallHTTP task, WorkflowDefinition definition) {
    HTTPArguments httpArgs = task.getWith();

    WorkflowApplication application = definition.application();

    this.authProvider =
        AuthProviderFactory.getAuth(
            application,
            definition.workflow(),
            task.getWith().getEndpoint().getEndpointConfiguration());

    this.targetSupplier = getTargetSupplier(definition, httpArgs.getEndpoint());
    this.headersMap =
        httpArgs.getHeaders() != null
            ? Optional.of(
                buildMapResolver(
                    application,
                    httpArgs.getHeaders().getRuntimeExpression(),
                    httpArgs.getHeaders().getHTTPHeaders() != null
                        ? httpArgs.getHeaders().getHTTPHeaders().getAdditionalProperties()
                        : null))
            : Optional.empty();
    this.queryMap =
        httpArgs.getQuery() != null
            ? Optional.of(
                buildMapResolver(
                    application,
                    httpArgs.getQuery().getRuntimeExpression(),
                    httpArgs.getQuery().getHTTPQuery() != null
                        ? httpArgs.getQuery().getHTTPQuery().getAdditionalProperties()
                        : null))
            : Optional.empty();
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.POST:
        WorkflowValueResolver<Map<String, Object>> bodyFilter =
            buildMapResolver(application, null, httpArgs.getBody());
        this.requestFunction =
            (request, w, context, node) ->
                converter.toModel(
                    application.modelFactory(),
                    node,
                    request.post(
                        converter.toEntity(bodyFilter.apply(w, context, node)),
                        node.objectClass()));
        break;
      case HttpMethod.GET:
      default:
        this.requestFunction =
            (request, w, t, n) ->
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
    authProvider.ifPresent(auth -> auth.build(request, workflow, taskContext, input));
    headersMap.ifPresent(
        h -> h.apply(workflow, taskContext, input).forEach((k, v) -> request.header(k, v)));
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            authProvider.ifPresent(auth -> auth.preRequest(request, workflow, taskContext, input));
            WorkflowModel result = requestFunction.apply(request, workflow, taskContext, input);
            authProvider.ifPresent(auth -> auth.postRequest(workflow, taskContext, input));
            return result;
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

  private static TargetSupplier getTargetSupplier(
      WorkflowDefinition definition, Endpoint endpoint) {
    return (w, t, n) ->
        client.target(definition.resourceLoader().uriSupplier(endpoint).apply(w, t, n));
  }

  private static interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  private static class ExpressionURISupplier implements TargetSupplier {
    private WorkflowValueResolver<String> expr;

    public ExpressionURISupplier(WorkflowValueResolver<String> expr) {
      this.expr = expr;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext task, WorkflowModel node) {
      return client.target(expr.apply(workflow, task, node));
    }
  }
}
