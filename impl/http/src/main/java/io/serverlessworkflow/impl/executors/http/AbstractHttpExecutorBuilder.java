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
package io.serverlessworkflow.impl.executors.http;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

abstract class AbstractHttpExecutorBuilder {

  protected WorkflowValueResolver<WebTarget> targetSupplier;
  protected WorkflowValueResolver<Map<String, Object>> headersMap;
  protected WorkflowValueResolver<Map<String, Object>> queryMap;
  protected Optional<AuthProvider> authProvider = Optional.empty();
  protected RequestSupplier requestFunction;
  protected boolean redirect;

  protected static RequestSupplier buildRequestSupplier(
      String method, Object body, WorkflowApplication application) {

    switch (method.toUpperCase()) {
      case HttpMethod.POST:
        WorkflowFilter bodyFilter = WorkflowUtils.buildWorkflowFilter(application, body);
        return (request, w, t, node) -> {
          HttpModelConverter converter = HttpConverterResolver.converter(w, t);
          return w.definition()
              .application()
              .modelFactory()
              .fromAny(
                  request.post(
                      converter.toEntity(bodyFilter.apply(w, t, node)), converter.responseType()));
        };
      case HttpMethod.GET:
      default:
        return (request, w, t, n) ->
            w.definition()
                .application()
                .modelFactory()
                .fromAny(request.get(HttpConverterResolver.converter(w, t).responseType()));
    }
  }

  protected static WorkflowValueResolver<WebTarget> getTargetSupplier(
      WorkflowValueResolver<URI> uriSupplier) {
    return (w, t, n) -> HttpClientResolver.client(w, t).target(uriSupplier.apply(w, t, n));
  }

  public HttpExecutor build() {
    return new HttpExecutor(
        targetSupplier,
        Optional.ofNullable(headersMap),
        Optional.ofNullable(queryMap),
        authProvider,
        requestFunction);
  }
}
