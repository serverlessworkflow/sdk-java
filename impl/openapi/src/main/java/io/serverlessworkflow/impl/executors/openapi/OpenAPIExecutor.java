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

import static io.serverlessworkflow.impl.executors.http.HttpExecutor.getTargetSupplier;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.http.HttpExecutor;
import io.serverlessworkflow.impl.executors.http.TargetSupplier;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {

  private CallOpenAPI task;
  private Workflow workflow;
  private WorkflowDefinition definition;
  private WorkflowApplication application;
  private TargetSupplier targetSupplier;
  private ResourceLoader resourceLoader;
  private OperationDefinitionSupplier operationDefinitionSupplier;

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    OperationDefinition operation =
        operationDefinitionSupplier.get(workflowContext, taskContext, input);

    return CompletableFuture.supplyAsync(
        () -> {
          HttpCallAdapter httpCallAdapter =
              getHttpCallAdapter(operation, workflowContext, taskContext, input);

          WorkflowException workflowException = null;

          for (var server : operation.getServers()) {
            CallHTTP callHTTP = httpCallAdapter.server(server).build();
            HttpExecutor executor = new HttpExecutor();
            executor.init(callHTTP, definition);

            try {
              return executor.apply(workflowContext, taskContext, input).get();
            } catch (WorkflowException e) {
              workflowException = e;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
          throw workflowException; // if we there, we failed all servers and ex is not null
        },
        workflowContext.definition().application().executorService());
  }

  @Override
  public void init(CallOpenAPI task, WorkflowDefinition definition) {
    this.task = task;
    this.definition = definition;
    this.workflow = definition.workflow();
    this.application = definition.application();
    this.resourceLoader = definition.resourceLoader();
    this.operationDefinitionSupplier = new OperationDefinitionSupplier(application, task);
    this.targetSupplier =
        getTargetSupplier(
            task.getWith().getDocument().getEndpoint(), application.expressionFactory());
  }

  private HttpCallAdapter getHttpCallAdapter(
      OperationDefinition operation,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    OperationPathResolver pathResolver =
        new OperationPathResolver(
            operation.getPath(),
            application,
            task.getWith().getParameters().getAdditionalProperties());

    return new HttpCallAdapter()
        .auth(task.getWith().getAuthentication())
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
        .redirect(task.getWith().isRedirect())
        .target(pathResolver.resolve(workflowContext, taskContext, input))
        .workflowParams(task.getWith().getParameters().getAdditionalProperties());
  }
}
