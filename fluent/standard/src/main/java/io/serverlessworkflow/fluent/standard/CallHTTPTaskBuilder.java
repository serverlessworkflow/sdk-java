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
package io.serverlessworkflow.fluent.standard;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.HTTPHeaders;
import io.serverlessworkflow.api.types.HTTPQuery;
import io.serverlessworkflow.api.types.Headers;
import io.serverlessworkflow.api.types.Query;
import io.serverlessworkflow.api.types.UriTemplate;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public class CallHTTPTaskBuilder extends TaskBaseBuilder<CallHTTPTaskBuilder> {

  private final CallHTTP callHTTP;

  CallHTTPTaskBuilder() {
    callHTTP = new CallHTTP();
    callHTTP.setWith(new HTTPArguments());
    callHTTP.getWith().setOutput(HTTPArguments.HTTPOutput.CONTENT);
    super.setTask(this.callHTTP);
  }

  @Override
  protected CallHTTPTaskBuilder self() {
    return this;
  }

  public CallHTTPTaskBuilder method(String method) {
    this.callHTTP.getWith().setMethod(method);
    return this;
  }

  public CallHTTPTaskBuilder endpoint(URI endpoint) {
    this.callHTTP
        .getWith()
        .setEndpoint(new Endpoint().withUriTemplate(new UriTemplate().withLiteralUri(endpoint)));
    return this;
  }

  public CallHTTPTaskBuilder endpoint(String expr) {
    this.callHTTP.getWith().setEndpoint(new Endpoint().withRuntimeExpression(expr));
    return this;
  }

  // TODO: add endpoint configuration to support authentication

  public CallHTTPTaskBuilder headers(String expr) {
    this.callHTTP.getWith().setHeaders(new Headers().withRuntimeExpression(expr));
    return this;
  }

  public CallHTTPTaskBuilder headers(Consumer<HTTPHeadersBuilder> consumer) {
    HTTPHeadersBuilder hb = new HTTPHeadersBuilder();
    consumer.accept(hb);
    callHTTP.getWith().setHeaders(hb.build());
    return this;
  }

  public CallHTTPTaskBuilder headers(Map<String, String> headers) {
    HTTPHeadersBuilder hb = new HTTPHeadersBuilder();
    hb.headers(headers);
    callHTTP.getWith().setHeaders(hb.build());
    return this;
  }

  public CallHTTPTaskBuilder body(Object body) {
    this.callHTTP.getWith().setBody(body);
    return this;
  }

  public CallHTTPTaskBuilder query(String expr) {
    this.callHTTP.getWith().setQuery(new Query().withRuntimeExpression(expr));
    return this;
  }

  public CallHTTPTaskBuilder query(Consumer<HTTPQueryBuilder> consumer) {
    HTTPQueryBuilder queryBuilder = new HTTPQueryBuilder();
    consumer.accept(queryBuilder);
    callHTTP.getWith().setQuery(queryBuilder.build());
    return this;
  }

  public CallHTTPTaskBuilder query(Map<String, String> query) {
    HTTPQueryBuilder httpQueryBuilder = new HTTPQueryBuilder();
    httpQueryBuilder.queries(query);
    callHTTP.getWith().setQuery(httpQueryBuilder.build());
    return this;
  }

  public CallHTTPTaskBuilder redirect(boolean redirect) {
    callHTTP.getWith().setRedirect(redirect);
    return this;
  }

  public CallHTTPTaskBuilder output(HTTPArguments.HTTPOutput output) {
    callHTTP.getWith().setOutput(output);
    return this;
  }

  public CallHTTP build() {
    return callHTTP;
  }

  public static class HTTPQueryBuilder {
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

  public static class HTTPHeadersBuilder {
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
