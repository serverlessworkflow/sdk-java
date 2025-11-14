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
package io.serverlessworkflow.fluent.spec.spi;

import io.serverlessworkflow.api.types.AuthenticationPolicyReference;
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
import io.serverlessworkflow.fluent.spec.ReferenceableAuthenticationPolicyBuilder;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public interface CallHttpTaskFluent<SELF extends TaskBaseBuilder<SELF>> {

  CallHTTP build();

  SELF self();

  default SELF method(String method) {
    ((CallHTTP) this.self().getTask()).getWith().setMethod(method);
    return self();
  }

  default SELF endpoint(URI endpoint) {
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setEndpoint(new Endpoint().withUriTemplate(new UriTemplate().withLiteralUri(endpoint)));
    return self();
  }

  default SELF endpoint(URI endpoint, Consumer<ReferenceableAuthenticationPolicyBuilder> auth) {
    final ReferenceableAuthenticationPolicyBuilder policy =
        new ReferenceableAuthenticationPolicyBuilder();
    auth.accept(policy);
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setEndpoint(
            new Endpoint()
                .withEndpointConfiguration(
                    new EndpointConfiguration().withAuthentication(policy.build()))
                .withUriTemplate(new UriTemplate().withLiteralUri(endpoint)));
    return self();
  }

  default SELF endpoint(String expr) {
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setEndpoint(new Endpoint().withRuntimeExpression(expr));
    return self();
  }

  default SELF endpoint(String expr, Consumer<ReferenceableAuthenticationPolicyBuilder> auth) {
    final ReferenceableAuthenticationPolicyBuilder policy =
        new ReferenceableAuthenticationPolicyBuilder();
    auth.accept(policy);
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setEndpoint(
            new Endpoint()
                .withEndpointConfiguration(
                    new EndpointConfiguration().withAuthentication(policy.build()))
                .withRuntimeExpression(expr));
    return self();
  }

  default SELF endpoint(String expr, String authUse) {
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setEndpoint(
            new Endpoint()
                .withEndpointConfiguration(
                    new EndpointConfiguration()
                        .withAuthentication(
                            new ReferenceableAuthenticationPolicy()
                                .withAuthenticationPolicyReference(
                                    new AuthenticationPolicyReference(authUse))))
                .withRuntimeExpression(expr));
    return self();
  }

  default SELF headers(String expr) {
    ((CallHTTP) this.self().getTask())
        .getWith()
        .setHeaders(new Headers().withRuntimeExpression(expr));
    return self();
  }

  default SELF headers(Consumer<HTTPHeadersBuilder> consumer) {
    HTTPHeadersBuilder hb = new HTTPHeadersBuilder();
    consumer.accept(hb);
    CallHTTP httpTask = ((CallHTTP) this.self().getTask());
    if (httpTask.getWith().getHeaders() != null
        && httpTask.getWith().getHeaders().getHTTPHeaders() != null) {
      Headers h = httpTask.getWith().getHeaders();
      Headers built = hb.build();
      built
          .getHTTPHeaders()
          .getAdditionalProperties()
          .forEach((k, v) -> h.getHTTPHeaders().setAdditionalProperty(k, v));
    } else {
      httpTask.getWith().setHeaders(hb.build());
    }

    return self();
  }

  default SELF headers(Map<String, String> headers) {
    HTTPHeadersBuilder hb = new HTTPHeadersBuilder();
    hb.headers(headers);
    ((CallHTTP) this.self().getTask()).getWith().setHeaders(hb.build());
    return self();
  }

  default SELF body(Object body) {
    ((CallHTTP) this.self().getTask()).getWith().setBody(body);
    return self();
  }

  default SELF query(String expr) {
    ((CallHTTP) this.self().getTask()).getWith().setQuery(new Query().withRuntimeExpression(expr));
    return self();
  }

  default SELF query(Consumer<HTTPQueryBuilder> consumer) {
    HTTPQueryBuilder queryBuilder = new HTTPQueryBuilder();
    consumer.accept(queryBuilder);
    ((CallHTTP) this.self().getTask()).getWith().setQuery(queryBuilder.build());
    return self();
  }

  default SELF query(Map<String, String> query) {
    HTTPQueryBuilder httpQueryBuilder = new HTTPQueryBuilder();
    httpQueryBuilder.queries(query);
    ((CallHTTP) this.self().getTask()).getWith().setQuery(httpQueryBuilder.build());
    return self();
  }

  default SELF redirect(boolean redirect) {
    ((CallHTTP) this.self().getTask()).getWith().setRedirect(redirect);
    return self();
  }

  default SELF output(HTTPArguments.HTTPOutput output) {
    ((CallHTTP) this.self().getTask()).getWith().setOutput(output);
    return self();
  }

  class HTTPQueryBuilder {
    private final HTTPQuery httpQuery = new HTTPQuery();

    public HTTPQueryBuilder query(String name, String value) {
      httpQuery.setAdditionalProperty(name, value);
      return this;
    }

    public HTTPQueryBuilder queries(Map<String, String> headers) {
      headers.forEach(httpQuery::setAdditionalProperty);
      return this;
    }

    public Query build() {
      return new Query().withHTTPQuery(httpQuery);
    }
  }

  class HTTPHeadersBuilder {
    private final HTTPHeaders httpHeaders = new HTTPHeaders();

    public HTTPHeadersBuilder header(String name, String value) {
      httpHeaders.setAdditionalProperty(name, value);
      return this;
    }

    public HTTPHeadersBuilder headers(Map<String, String> headers) {
      headers.forEach(httpHeaders::setAdditionalProperty);
      return this;
    }

    public Headers build() {
      return new Headers().withHTTPHeaders(httpHeaders);
    }
  }
}
