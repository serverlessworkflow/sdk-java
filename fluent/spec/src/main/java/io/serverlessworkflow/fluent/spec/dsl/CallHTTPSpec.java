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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.fluent.spec.CallHTTPTaskBuilder;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.CallHTTPConfigurer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CallHTTPSpec implements CallHTTPConfigurer {

  private final List<CallHTTPConfigurer> steps = new ArrayList<>();

  public CallHTTPSpec GET() {
    steps.add(c -> c.method("GET"));
    return this;
  }

  public CallHTTPSpec POST() {
    steps.add(c -> c.method("POST"));
    return this;
  }

  public CallHTTPSpec acceptJSON() {
    return header("Accept", "application/json");
  }

  public CallHTTPSpec endpoint(String urlExpr) {
    steps.add(b -> b.endpoint(urlExpr));
    return this;
  }

  public CallHTTPSpec endpoint(String urlExpr, AuthenticationConfigurer auth) {
    steps.add(b -> b.endpoint(urlExpr, auth));
    return this;
  }

  public CallHTTPSpec uri(String url) {
    steps.add(b -> b.endpoint(URI.create(url)));
    return this;
  }

  public CallHTTPSpec uri(String url, AuthenticationConfigurer auth) {
    steps.add(b -> b.endpoint(URI.create(url), auth));
    return this;
  }

  public CallHTTPSpec uri(URI uri) {
    steps.add(b -> b.endpoint(uri));
    return this;
  }

  public CallHTTPSpec uri(URI uri, AuthenticationConfigurer auth) {
    steps.add(b -> b.endpoint(uri, auth));
    return this;
  }

  public CallHTTPSpec body(String bodyExpr) {
    steps.add(c -> c.body(bodyExpr));
    return this;
  }

  public CallHTTPSpec body(Map<String, Object> body) {
    steps.add(c -> c.body(body));
    return this;
  }

  public CallHTTPSpec method(String method) {
    steps.add(b -> b.method(method));
    return this;
  }

  public CallHTTPSpec header(String name, String value) {
    steps.add(c -> c.headers(h -> h.header(name, value)));
    return this;
  }

  public CallHTTPSpec headers(Map<String, String> headers) {
    steps.add(b -> b.headers(headers));
    return this;
  }

  @Override
  public void accept(CallHTTPTaskBuilder b) {
    for (var s : steps) s.accept(b);
  }
}
