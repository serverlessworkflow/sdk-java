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
package io.serverlessworkflow.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.api.types.WithHTTPHeaders;
import io.serverlessworkflow.api.types.WithHTTPQuery;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class HttpExecutor extends AbstractTaskExecutor<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  private final Function<JsonNode, WebTarget> targetSupplier;

  public HttpExecutor(CallHTTP task, ExpressionFactory factory) {
    super(task, factory);
    this.targetSupplier = getTargetSupplier(task.getWith().getEndpoint());
  }

  @Override
  protected JsonNode internalExecute(JsonNode node) {
    HTTPArguments httpArgs = task.getWith();
    WithHTTPQuery query = httpArgs.getQuery();
    WebTarget target = targetSupplier.apply(node);
    if (query != null) {
      for (Entry<String, Object> entry : query.getAdditionalProperties().entrySet()) {
        target = target.queryParam(entry.getKey(), entry.getValue());
      }
    }
    Builder request = target.request();
    WithHTTPHeaders headers = httpArgs.getHeaders();
    if (headers != null) {
      headers.getAdditionalProperties().forEach(request::header);
    }
    switch (httpArgs.getMethod().toUpperCase()) {
      case HttpMethod.GET:
      default:
        return request.get(JsonNode.class);
      case HttpMethod.POST:
        return request.post(Entity.json(httpArgs.getBody()), JsonNode.class);
    }
  }

  private Function<JsonNode, WebTarget> getTargetSupplier(Endpoint endpoint) {
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

  private Function<JsonNode, WebTarget> getURISupplier(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return new URISupplier(template.getLiteralUri());
    } else if (template.getLiteralUriTemplate() != null) {
      return new URITemplateSupplier(template.getLiteralUriTemplate());
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private class URISupplier implements Function<JsonNode, WebTarget> {
    private final URI uri;

    public URISupplier(URI uri) {
      this.uri = uri;
    }

    @Override
    public WebTarget apply(JsonNode input) {
      return client.target(uri);
    }
  }

  private class URITemplateSupplier implements Function<JsonNode, WebTarget> {
    private final String uri;

    public URITemplateSupplier(String uri) {
      this.uri = uri;
    }

    @Override
    public WebTarget apply(JsonNode input) {
      return client
          .target(uri)
          .resolveTemplates(
              JsonUtils.mapper().convertValue(input, new TypeReference<Map<String, Object>>() {}));
    }
  }

  private class ExpressionURISupplier implements Function<JsonNode, WebTarget> {
    private Expression expr;

    public ExpressionURISupplier(String expr) {
      this.expr = exprFactory.getExpression(expr);
    }

    @Override
    public WebTarget apply(JsonNode input) {
      return client.target(expr.eval(input).asText());
    }
  }
}
