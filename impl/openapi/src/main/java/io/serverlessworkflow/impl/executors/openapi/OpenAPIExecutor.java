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
import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.http.HttpExecutor;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {

  private OperationDefinitionSupplier operationDefinitionSupplier;
  private OpenAPIArguments with;

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    OperationDefinition operation =
        operationDefinitionSupplier.get(workflowContext, taskContext, input);
    HttpCallAdapter httpCallAdapter =
        getHttpCallAdapter(operation, workflowContext, taskContext, input);

    Iterator<String> iter = operation.getServers().iterator();
    if (!iter.hasNext()) {
      throw new IllegalArgumentException(
          "List of servers is empty for operation " + operation.getOperation());
    }
    CompletableFuture<WorkflowModel> future =
        executeServer(iter.next(), httpCallAdapter, workflowContext, taskContext, input);
    while (iter.hasNext()) {
      future.exceptionallyCompose(
          i -> executeServer(iter.next(), httpCallAdapter, workflowContext, taskContext, input));
    }
    return future;
  }

  private CompletableFuture<WorkflowModel> executeServer(
      String server,
      HttpCallAdapter callAdapter,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    CallHTTP callHTTP = callAdapter.server(server).build();
    HttpExecutor executor = new HttpExecutor();
    executor.init(callHTTP, workflowContext.definition());
    return executor.apply(workflowContext, taskContext, input);
  }

  @Override
  public void init(CallOpenAPI task, WorkflowDefinition definition) {
    with = task.getWith();
    operationDefinitionSupplier = new OperationDefinitionSupplier(definition.application(), with);
  }

  private HttpCallAdapter getHttpCallAdapter(
      OperationDefinition operation,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    OperationPathResolver pathResolver =
        new OperationPathResolver(
            operation.getPath(),
            workflowContext.definition().application(),
            with.getParameters().getAdditionalProperties());

    return new HttpCallAdapter()
        .auth(with.getAuthentication())
        .body(operation.getBody())
        .contentType(operation.getContentType())
        .headers(
            operation.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()))
                .collect(Collectors.toUnmodifiableSet()))
        .method(operation.getMethod())
        .query(
            operation.getParameters().stream()
                .filter(p -> "query".equals(p.getIn()))
                .collect(Collectors.toUnmodifiableSet()))
        .redirect(with.isRedirect())
        .target(pathResolver.resolve(workflowContext, taskContext, input))
        .workflowParams(with.getParameters().getAdditionalProperties());
  }
}
