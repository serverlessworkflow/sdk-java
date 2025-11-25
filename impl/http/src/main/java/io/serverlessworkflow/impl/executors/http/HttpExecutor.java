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
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class HttpExecutor implements CallableTask {

  private final WorkflowValueResolver<WebTarget> targetSupplier;
  private final Optional<WorkflowValueResolver<Map<String, Object>>> headersMap;
  private final Optional<WorkflowValueResolver<Map<String, Object>>> queryMap;
  private final Optional<AuthProvider> authProvider;
  private final RequestSupplier requestFunction;

  HttpExecutor(
      WorkflowValueResolver<WebTarget> targetSupplier,
      Optional<WorkflowValueResolver<Map<String, Object>>> headersMap,
      Optional<WorkflowValueResolver<Map<String, Object>>> queryMap,
      Optional<AuthProvider> authProvider,
      RequestSupplier requestFunction) {
    this.targetSupplier = targetSupplier;
    this.headersMap = headersMap;
    this.queryMap = queryMap;
    this.authProvider = authProvider;
    this.requestFunction = requestFunction;
  }

  private static class TargetQuerySupplier implements Supplier<WebTarget> {

    private WebTarget target;

    public TargetQuerySupplier(WebTarget original) {
      this.target = original;
    }

    public void addQuery(String key, Object value) {
      target = target.queryParam(key, value);
    }

    public WebTarget get() {
      return target;
    }
  }

  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflow, TaskContext taskContext, WorkflowModel input) {
    TargetQuerySupplier supplier =
        new TargetQuerySupplier(targetSupplier.apply(workflow, taskContext, input));
    queryMap.ifPresent(
        q -> q.apply(workflow, taskContext, input).forEach((k, v) -> supplier.addQuery(k, v)));
    Builder request = supplier.get().request();
    headersMap.ifPresent(
        h -> h.apply(workflow, taskContext, input).forEach((k, v) -> request.header(k, v)));
    return CompletableFuture.supplyAsync(
        () -> {
          authProvider.ifPresent(auth -> auth.build(request, workflow, taskContext, input));
          return requestFunction.apply(request, workflow, taskContext, input);
        },
        workflow.definition().application().executorService());
  }
}
