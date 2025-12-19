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

import static io.serverlessworkflow.impl.WorkflowUtils.buildMapResolver;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;
import java.net.URI;

public class CallableTaskHttpExecutorBuilder implements CallableTaskBuilder<CallHTTP> {

  private HttpExecutorBuilder builder;
  private WorkflowValueResolver<URI> uriSupplier;

  @Override
  public void init(CallHTTP task, WorkflowDefinition definition, WorkflowMutablePosition position) {

    builder = HttpExecutorBuilder.builder(definition);
    final HTTPArguments httpArgs = task.getWith();
    final Endpoint endpoint = httpArgs.getEndpoint();

    if (endpoint.getEndpointConfiguration() != null) {
      builder.withAuth(endpoint.getEndpointConfiguration().getAuthentication());
    }

    uriSupplier = definition.resourceLoader().uriSupplier(endpoint);

    if (httpArgs.getHeaders() != null) {
      builder.withHeaders(
          buildMapResolver(
              definition.application(),
              httpArgs.getHeaders().getRuntimeExpression(),
              httpArgs.getHeaders().getHTTPHeaders() != null
                  ? httpArgs.getHeaders().getHTTPHeaders().getAdditionalProperties()
                  : null));
    }

    if (httpArgs.getQuery() != null) {
      builder.withQueryMap(
          buildMapResolver(
              definition.application(),
              httpArgs.getQuery().getRuntimeExpression(),
              httpArgs.getQuery().getHTTPQuery() != null
                  ? httpArgs.getQuery().getHTTPQuery().getAdditionalProperties()
                  : null));
    }

    builder.withBody(httpArgs.getBody());
    builder.withMethod(httpArgs.getMethod().toUpperCase());
    builder.redirect(httpArgs.isRedirect());
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallHTTP.class);
  }

  @Override
  public CallableTask build() {
    return builder.build(uriSupplier);
  }
}
