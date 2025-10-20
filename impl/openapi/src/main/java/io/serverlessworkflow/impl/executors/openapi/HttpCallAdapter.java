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
package io.serverlessworkflow.impl.executors.openapi;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointConfiguration;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.HTTPHeaders;
import io.serverlessworkflow.api.types.HTTPQuery;
import io.serverlessworkflow.api.types.Headers;
import io.serverlessworkflow.api.types.Query;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.UriTemplate;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
class HttpCallAdapter {

  private ReferenceableAuthenticationPolicy auth;
  private Map<String, Schema> body;
  private String contentType;
  private Collection<Parameter> headers;
  private String method;
  private Collection<Parameter> query;
  private boolean redirect;
  private URI server;
  private URI target;
  private Map<String, Object> workflowParams;

  HttpCallAdapter auth(ReferenceableAuthenticationPolicy policy) {
    if (policy != null) {
      this.auth = policy;
    }
    return this;
  }

  HttpCallAdapter body(Map<String, Schema> body) {
    this.body = body;
    return this;
  }

  CallHTTP build() {
    CallHTTP callHTTP = new CallHTTP();

    HTTPArguments httpArgs = new HTTPArguments();
    callHTTP.withWith(httpArgs);

    Endpoint endpoint = new Endpoint();
    httpArgs.withEndpoint(endpoint);

    if (this.auth != null) {
      EndpointConfiguration endPointConfig = new EndpointConfiguration();
      endPointConfig.setAuthentication(this.auth);
      endpoint.setEndpointConfiguration(endPointConfig);
    }

    httpArgs.setRedirect(this.redirect);
    httpArgs.setMethod(this.method);

    addHttpHeaders(httpArgs);
    addQueryParams(httpArgs);
    addBody(httpArgs);

    addTarget(endpoint);

    return callHTTP;
  }

  HttpCallAdapter contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  HttpCallAdapter headers(Collection<io.swagger.v3.oas.models.parameters.Parameter> headers) {
    this.headers = headers;
    return this;
  }

  HttpCallAdapter method(String method) {
    this.method = method;
    return this;
  }

  HttpCallAdapter query(Collection<io.swagger.v3.oas.models.parameters.Parameter> query) {
    this.query = query;
    return this;
  }

  HttpCallAdapter redirect(boolean redirect) {
    this.redirect = redirect;
    return this;
  }

  HttpCallAdapter server(String server) {
    this.server = URI.create(server);
    return this;
  }

  HttpCallAdapter target(URI target) {
    this.target = target;
    return this;
  }

  HttpCallAdapter workflowParams(Map<String, Object> workflowParams) {
    this.workflowParams = workflowParams;
    return this;
  }

  private void addBody(HTTPArguments httpArgs) {
    Map<String, Object> bodyContent = new LinkedHashMap<>();
    if (!(body == null || body.isEmpty())) {
      for (Map.Entry<String, Schema> entry : body.entrySet()) {
        String name = entry.getKey();
        if (workflowParams.containsKey(name)) {
          Object value = workflowParams.get(name);
          bodyContent.put(name, value);
        }
      }
      if (!bodyContent.isEmpty()) {
        httpArgs.setBody(bodyContent);
      }
    }
  }

  private void addHttpHeaders(HTTPArguments httpArgs) {
    if (!(headers == null || headers.isEmpty())) {
      Headers hdrs = new Headers();
      HTTPHeaders httpHeaders = new HTTPHeaders();
      hdrs.setHTTPHeaders(httpHeaders);
      httpArgs.setHeaders(hdrs);

      for (Parameter p : headers) {
        String name = p.getName();
        if (workflowParams.containsKey(name)) {
          Object value = workflowParams.get(name);
          if (value instanceof String asString) {
            httpHeaders.setAdditionalProperty(name, asString);
          } else {
            throw new IllegalArgumentException("Header parameter " + name + " must be a String");
          }
        }
      }
    }
  }

  private void addQueryParams(HTTPArguments httpArgs) {
    if (!(query == null || query.isEmpty())) {
      Query queryParams = new Query();
      httpArgs.setQuery(queryParams);
      HTTPQuery httpQuery = new HTTPQuery();
      queryParams.setHTTPQuery(httpQuery);

      for (Parameter p : query) {
        String name = p.getName();
        if (workflowParams.containsKey(name)) {
          Object value = workflowParams.get(name);
          if (value instanceof String asString) {
            httpQuery.setAdditionalProperty(name, asString);
          } else if (value instanceof Number asNumber) {
            httpQuery.setAdditionalProperty(name, asNumber.toString());
          } else if (value instanceof Boolean asBoolean) {
            httpQuery.setAdditionalProperty(name, asBoolean.toString());
          } else if (value instanceof Character asCharacter) {
            httpQuery.setAdditionalProperty(name, asCharacter.toString());
          } else {
            httpQuery.setAdditionalProperty(name, value.toString());
          }
        }
      }
    }
  }

  private void addTarget(Endpoint endpoint) {
    if (this.target == null) {
      throw new IllegalArgumentException("No Server defined for the OpenAPI operation");
    }
    UriTemplate uriTemplate = new UriTemplate();
    uriTemplate.withLiteralUri(this.server.resolve(this.target.getPath()));
    endpoint.setUriTemplate(uriTemplate);
  }
}
