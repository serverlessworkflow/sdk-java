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
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.json.JsonUtils;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Map.Entry;

public class HttpExecutor implements CallableTask<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private TargetSupplier targetSupplier;
  private Map<String, Object> headersMap;
  private Map<String, Object> queryMap;
  private RequestSupplier requestFunction;

  @FunctionalInterface
  private interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node);
  }

  @FunctionalInterface
  private interface RequestSupplier {
    JsonNode apply(Builder request, WorkflowContext workflow, TaskContext<?> task, JsonNode node);
  }

  @Override
  public void init(CallHTTP task, WorkflowDefinition definition) {
    HTTPArguments httpArgs = task.getWith();
    this.targetSupplier = getTargetSupplier(httpArgs.getEndpoint(), definition.expressionFactory());
    this.headersMap =
        httpArgs.getHeaders() != null
            ? ExpressionUtils.buildExpressionMap(
                httpArgs.getHeaders().getAdditionalProperties(), definition.expressionFactory())
            : Map.of();
    this.queryMap =
        httpArgs.getQuery() != null
            ? ExpressionUtils.buildExpressionMap(
                httpArgs.getQuery().getAdditionalProperties(), definition.expressionFactory())
            : Map.of();
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.POST:
        Object body =
            ExpressionUtils.buildExpressionObject(
                httpArgs.getBody(), definition.expressionFactory());
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
  public JsonNode apply(
      WorkflowContext workflow, TaskContext<CallHTTP> taskContext, JsonNode input) {
    WebTarget target = targetSupplier.apply(workflow, taskContext, input);
    for (Entry<String, Object> entry :
        ExpressionUtils.evaluateExpressionMap(queryMap, workflow, taskContext, input).entrySet()) {
      target = target.queryParam(entry.getKey(), entry.getValue());
    }
    Builder request = target.request();
    ExpressionUtils.evaluateExpressionMap(headersMap, workflow, taskContext, input)
        .forEach(request::header);
    return requestFunction.apply(request, workflow, taskContext, input);
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
    public WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
      return client.target(expr.eval(workflow, task, node).asText());
    }
  }
}
