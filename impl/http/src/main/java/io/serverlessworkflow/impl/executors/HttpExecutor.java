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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HttpExecutor implements CallableTask<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private TargetSupplier targetSupplier;
  private Optional<WorkflowFilter> headersMap;
  private Optional<WorkflowFilter> queryMap;
  private RequestSupplier requestFunction;

  @FunctionalInterface
  private interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext task, JsonNode node);
  }

  @FunctionalInterface
  private interface RequestSupplier {
    JsonNode apply(Builder request, WorkflowContext workflow, TaskContext task, JsonNode node);
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
                    application.expressionFactory(),
                    httpArgs.getHeaders().getRuntimeExpression(),
                    httpArgs.getHeaders().getHTTPHeaders() != null
                        ? httpArgs.getHeaders().getHTTPHeaders().getAdditionalProperties()
                        : null))
            : Optional.empty();
    this.queryMap =
        httpArgs.getQuery() != null
            ? Optional.of(
                WorkflowUtils.buildWorkflowFilter(
                    application.expressionFactory(),
                    httpArgs.getQuery().getRuntimeExpression(),
                    httpArgs.getQuery().getHTTPQuery() != null
                        ? httpArgs.getQuery().getHTTPQuery().getAdditionalProperties()
                        : null))
            : Optional.empty();
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.POST:
        Object body =
            ExpressionUtils.buildExpressionObject(
                httpArgs.getBody(), application.expressionFactory());
        this.requestFunction =
            (request, workflow, context, node) ->
                request.post(
                    Entity.json(
                        ExpressionUtils.evaluateExpressionObject(body, workflow, context, node)),
                    JsonNode.class);
        break;
      case HttpMethod.GET:
      default:
        this.requestFunction = (request, w, t, n) -> request.get(JsonNode.class);
    }
  }

  @Override
  public CompletableFuture<JsonNode> apply(
      WorkflowContext workflow, TaskContext taskContext, JsonNode input) {
    WebTarget target = targetSupplier.apply(workflow, taskContext, input);
    Optional<JsonNode> queryJson = queryMap.map(q -> q.apply(workflow, taskContext, input));
    if (queryJson.isPresent()) {
      Iterator<Entry<String, JsonNode>> iter = queryJson.orElseThrow().fields();
      while (iter.hasNext()) {
        Entry<String, JsonNode> item = iter.next();
        target = target.queryParam(item.getKey(), JsonUtils.toJavaValue(item.getValue()));
      }
    }

    Builder request = target.request();
    headersMap.ifPresent(
        h ->
            h.apply(workflow, taskContext, input)
                .fields()
                .forEachRemaining(
                    e -> request.header(e.getKey(), JsonUtils.toJavaValue(e.getValue()))));
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
            expressionFactory.getExpression(uri.getExpressionEndpointURI()));
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(
          expressionFactory.getExpression(endpoint.getRuntimeExpression()));
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
          client
              .target(template.getLiteralUriTemplate())
              .resolveTemplates(
                  JsonUtils.mapper().convertValue(n, new TypeReference<Map<String, Object>>() {}));
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private static class ExpressionURISupplier implements TargetSupplier {
    private Expression expr;

    public ExpressionURISupplier(Expression expr) {
      this.expr = expr;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext task, JsonNode node) {
      return client.target(expr.eval(workflow, task, node).asText());
    }
  }
}
