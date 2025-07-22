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
package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HttpExecutor implements CallableTask<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private TargetSupplier targetSupplier;
  private Optional<WorkflowFilter> headersMap;
  private Optional<WorkflowFilter> queryMap;
  private RequestSupplier requestFunction;
  private static HttpModelConverter converter = new HttpModelConverter() {};

  @FunctionalInterface
  private interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  @FunctionalInterface
  private interface RequestSupplier {
    WorkflowModel apply(
        Builder request, WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  @Override
  public void init(CallHTTP task, WorkflowApplication application, ResourceLoader resourceLoader) {
    HTTPArguments httpArgs = task.getWith();
    this.targetSupplier =
        getTargetSupplier(httpArgs.getEndpoint(), application.expressionFactory());
    this.headersMap =
        httpArgs.getHeaders() != null
            ? Optional.of(
                WorkflowUtils.buildWorkflowFilter(
                    application,
                    httpArgs.getHeaders().getRuntimeExpression(),
                    httpArgs.getHeaders().getHTTPHeaders() != null
                        ? httpArgs.getHeaders().getHTTPHeaders().getAdditionalProperties()
                        : null))
            : Optional.empty();
    this.queryMap =
        httpArgs.getQuery() != null
            ? Optional.of(
                WorkflowUtils.buildWorkflowFilter(
                    application,
                    httpArgs.getQuery().getRuntimeExpression(),
                    httpArgs.getQuery().getHTTPQuery() != null
                        ? httpArgs.getQuery().getHTTPQuery().getAdditionalProperties()
                        : null))
            : Optional.empty();
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.POST:
        WorkflowFilter bodyFilter =
            WorkflowUtils.buildWorkflowFilter(application, null, httpArgs.getBody());
        this.requestFunction =
            (request, workflow, context, node) ->
                converter.toModel(
                    application.modelFactory(),
                    request.post(
                        converter.toEntity(bodyFilter.apply(workflow, context, node)),
                        node.objectClass()));
        break;
      case HttpMethod.GET:
      default:
        this.requestFunction =
            (request, w, t, n) ->
                converter.toModel(application.modelFactory(), request.get(n.objectClass()));
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
        q ->
            q.apply(workflow, taskContext, input)
                .forEach((k, v) -> supplier.addQuery(k, v.asJavaObject())));
    Builder request = supplier.get().request();
    headersMap.ifPresent(
        h ->
            h.apply(workflow, taskContext, input)
                .forEach((k, v) -> request.header(k, v.asJavaObject())));
    return CompletableFuture.supplyAsync(
        () -> {
          try {
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

  private static TargetSupplier getTargetSupplier(
      Endpoint endpoint, ExpressionFactory expressionFactory) {
    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURISupplier(uri.getLiteralEndpointURI());
      } else if (uri.getExpressionEndpointURI() != null) {
        return new ExpressionURISupplier(
            expressionFactory.buildExpression(uri.getExpressionEndpointURI()));
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(
          expressionFactory.buildExpression(endpoint.getRuntimeExpression()));
    } else if (endpoint.getUriTemplate() != null) {
      return getURISupplier(endpoint.getUriTemplate());
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }

  private static TargetSupplier getURISupplier(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return (w, t, n) -> client.target(template.getLiteralUri());
    } else if (template.getLiteralUriTemplate() != null) {
      return (w, t, n) ->
          client.target(template.getLiteralUriTemplate()).resolveTemplates(n.asMap().orElseThrow());
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private static class ExpressionURISupplier implements TargetSupplier {
    private Expression expr;

    public ExpressionURISupplier(Expression expr) {
      this.expr = expr;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext task, WorkflowModel node) {
      return client.target(
          expr.eval(workflow, task, node)
              .asText()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Target expression requires a string result")));
    }
  }
}
