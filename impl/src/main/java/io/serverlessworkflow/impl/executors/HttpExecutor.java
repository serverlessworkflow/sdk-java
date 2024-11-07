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
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
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
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

public class HttpExecutor extends AbstractTaskExecutor<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private final TargetSupplier targetSupplier;
  private final Map<String, Object> headersMap;
  private final Map<String, Object> queryMap;
  private final RequestSupplier requestFunction;

  @FunctionalInterface
  private interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node);
  }

  @FunctionalInterface
  private interface RequestSupplier {
    JsonNode apply(Builder request, WorkflowContext workflow, TaskContext<?> task, JsonNode node);
  }

  public HttpExecutor(CallHTTP task, ExpressionFactory factory) {
    super(task, factory);
    HTTPArguments httpArgs = task.getWith();
    this.targetSupplier = getTargetSupplier(httpArgs.getEndpoint());
    this.headersMap =
        httpArgs.getHeaders() != null
            ? ExpressionUtils.buildExpressionMap(
                httpArgs.getHeaders().getAdditionalProperties(), factory)
            : Map.of();
    this.queryMap =
        httpArgs.getQuery() != null
            ? ExpressionUtils.buildExpressionMap(
                httpArgs.getQuery().getAdditionalProperties(), factory)
            : Map.of();
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.POST:
        Object body = ExpressionUtils.buildExpressionObject(httpArgs.getBody(), factory);
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
  protected JsonNode internalExecute(
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

  private TargetSupplier getTargetSupplier(Endpoint endpoint) {
    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURISupplier(uri.getLiteralEndpointURI());
      } else if (uri.getExpressionEndpointURI() != null) {
        return new ExpressionURISupplier(uri.getExpressionEndpointURI());
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(endpoint.getRuntimeExpression());
    } else if (endpoint.getUriTemplate() != null) {
      return getURISupplier(endpoint.getUriTemplate());
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }

  private TargetSupplier getURISupplier(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return new URISupplier(template.getLiteralUri());
    } else if (template.getLiteralUriTemplate() != null) {
      return new URITemplateSupplier(template.getLiteralUriTemplate());
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private class URISupplier implements TargetSupplier {
    private final URI uri;

    public URISupplier(URI uri) {
      this.uri = uri;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
      return client.target(uri);
    }
  }

  private class URITemplateSupplier implements TargetSupplier {
    private final String uri;

    public URITemplateSupplier(String uri) {
      this.uri = uri;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
      return client
          .target(uri)
          .resolveTemplates(
              JsonUtils.mapper().convertValue(node, new TypeReference<Map<String, Object>>() {}));
    }
  }

  private class ExpressionURISupplier implements TargetSupplier {
    private Expression expr;

    public ExpressionURISupplier(String expr) {
      this.expr = exprFactory.getExpression(expr);
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
      return client.target(expr.eval(workflow, task, node).asText());
    }
  }
}
