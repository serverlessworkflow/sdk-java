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

import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.fluent.spec.spi.CallHttpTaskFluent;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface BaseCallHttpSpec<SELF extends BaseCallHttpSpec<SELF>> {

  /** Implementors must return the concrete SELF instance (usually {@code return this;}). */
  SELF self();

  /**
   * Internal list of configuration steps that will be replayed against the underlying {@link
   * CallHttpTaskFluent}.
   */
  List<Consumer<CallHttpTaskFluent<?>>> steps();

  default SELF GET() {
    steps().add(c -> c.method("GET"));
    return self();
  }

  default SELF POST() {
    steps().add(c -> c.method("POST"));
    return self();
  }

  default SELF acceptJSON() {
    return header("Accept", "application/json");
  }

  default SELF endpoint(String urlExpr) {
    steps().add(b -> b.endpoint(urlExpr));
    return self();
  }

  default SELF endpoint(String urlExpr, AuthenticationConfigurer auth) {
    steps().add(b -> b.endpoint(urlExpr, auth));
    return self();
  }

  default SELF uri(String url) {
    steps().add(b -> b.endpoint(URI.create(url)));
    return self();
  }

  default SELF uri(String url, AuthenticationConfigurer auth) {
    steps().add(b -> b.endpoint(URI.create(url), auth));
    return self();
  }

  default SELF uri(URI uri) {
    steps().add(b -> b.endpoint(uri));
    return self();
  }

  default SELF uri(URI uri, AuthenticationConfigurer auth) {
    steps().add(b -> b.endpoint(uri, auth));
    return self();
  }

  default SELF body(String bodyExpr) {
    steps().add(c -> c.body(bodyExpr));
    return self();
  }

  default SELF body(Map<String, Object> body) {
    steps().add(c -> c.body(body));
    return self();
  }

  default SELF body(Object bodyExpr) {
    steps().add(c -> c.body(bodyExpr));
    return self();
  }

  default SELF method(String method) {
    steps().add(b -> b.method(method));
    return self();
  }

  default SELF header(String name, String value) {
    steps().add(c -> c.headers(h -> h.header(name, value)));
    return self();
  }

  default SELF headers(Map<String, String> headers) {
    steps().add(b -> b.headers(headers));
    return self();
  }

  default void accept(CallHttpTaskFluent<?> b) {
    for (var s : steps()) {
      s.accept(b);
    }
  }
}
