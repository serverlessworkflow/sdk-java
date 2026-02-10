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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public class HttpExecutor implements CallableTask {

  private final WorkflowValueResolver<URI> uriSupplier;
  private final Optional<WorkflowValueResolver<URI>> pathSupplier;
  private final Optional<WorkflowValueResolver<Map<String, Object>>> headersMap;
  private final Optional<WorkflowValueResolver<Map<String, Object>>> queryMap;
  private final RequestExecutor requestFunction;
  private final Collection<HttpRequestDecorator> requestDecorators;

  HttpExecutor(
      WorkflowValueResolver<URI> uriSupplier,
      Optional<WorkflowValueResolver<Map<String, Object>>> headersMap,
      Optional<WorkflowValueResolver<Map<String, Object>>> queryMap,
      RequestExecutor requestFunction,
      Optional<WorkflowValueResolver<URI>> pathSupplier) {
    this.uriSupplier = uriSupplier;
    this.headersMap = headersMap;
    this.queryMap = queryMap;
    this.requestFunction = requestFunction;
    this.pathSupplier = pathSupplier;
    this.requestDecorators =
        ServiceLoader.load(HttpRequestDecorator.class).stream()
            .map(ServiceLoader.Provider::get)
            .sorted()
            .toList();
  }

  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflow, TaskContext taskContext, WorkflowModel input) {
    URI uri =
        pathSupplier
            .map(
                p ->
                    WorkflowUtils.concatURI(
                        uriSupplier.apply(workflow, taskContext, input),
                        p.apply(workflow, taskContext, input)))
            .orElse(uriSupplier.apply(workflow, taskContext, input));

    Client client = HttpClientResolver.client(workflow, taskContext);

    WebTarget target = client.target(uri);

    for (Entry<String, Object> entry :
        queryMap.map(q -> q.apply(workflow, taskContext, input)).orElse(Map.of()).entrySet()) {
      target = target.queryParam(entry.getKey(), entry.getValue());
    }
    Builder request = target.request();
    requestDecorators.forEach(d -> d.decorate(request, workflow, taskContext));
    headersMap.ifPresent(h -> h.apply(workflow, taskContext, input).forEach(request::header));
    return CompletableFuture.supplyAsync(
        () -> requestFunction.apply(request, uri, workflow, taskContext, input),
        workflow.definition().application().executorService());
  }
}
