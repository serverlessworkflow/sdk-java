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
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.http.HttpExecutor;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {

  private CallOpenAPI task;
  private Workflow workflow;
  private WorkflowDefinition definition;
  private WorkflowApplication application;
  private TargetSupplier targetSupplier;

  private ResourceLoader resourceLoader;

  private static TargetSupplier getTargetSupplier(
      Endpoint endpoint, ExpressionFactory expressionFactory) {
    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURISupplier(uri.getLiteralEndpointURI());
      } else if (uri.getExpressionEndpointURI() != null) {
        return new ExpressionURISupplier(
            expressionFactory.resolveString(
                ExpressionDescriptor.from(uri.getExpressionEndpointURI())));
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(
          expressionFactory.resolveString(
              ExpressionDescriptor.from(endpoint.getRuntimeExpression())));
    } else if (endpoint.getUriTemplate() != null) {
      return getURISupplier(endpoint.getUriTemplate());
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }

  private static TargetSupplier getURISupplier(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return (w, t, n) -> template.getLiteralUri();
    } else if (template.getLiteralUriTemplate() != null) {
      return (w, t, n) ->
          UriBuilder.fromUri(template.getLiteralUriTemplate())
              .resolveTemplates(n.asMap().orElseThrow(), false)
              .build();
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    String operationId = task.getWith().getOperationId();
    URI openAPIEndpoint = targetSupplier.apply(workflowContext, taskContext, input);
    OpenAPIProcessor processor = new OpenAPIProcessor(operationId, openAPIEndpoint);
    OperationDefinition operation = processor.parse();

    OperationPathResolver pathResolver =
        new OperationPathResolver(operation.getPath(), input.asMap().orElseThrow());
    URI resolvedPath = pathResolver.passPathParams().apply(workflowContext, taskContext, input);

    HttpCallAdapter httpCallAdapter =
        new HttpCallAdapter()
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
            .target(resolvedPath)
            .workflowParams(task.getWith().getParameters().getAdditionalProperties());

    return CompletableFuture.supplyAsync(
        () -> {
          RuntimeException ex = null;
          for (var server : operation.getServers()) {
            CallHTTP callHTTP = httpCallAdapter.server(server).build();
            HttpExecutor executor = new HttpExecutor();
            executor.init(callHTTP, definition);

            try {
              return executor.apply(workflowContext, taskContext, input).get();
            } catch (Exception e) {

              System.out.println("Call to " + server + " failed: " + e.getMessage());
              ex = new RuntimeException(e);
            }
          }
          Objects.requireNonNull(ex, "Should have at least one exception");
          throw ex; // if we there, we failed all servers and ex is not null
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

    this.targetSupplier =
        getTargetSupplier(
            task.getWith().getDocument().getEndpoint(), application.expressionFactory());
  }

  public interface TargetSupplier {
    URI apply(WorkflowContext workflow, TaskContext taskContext, WorkflowModel input);
  }

  private static class ExpressionURISupplier implements TargetSupplier {
    private WorkflowValueResolver<String> expr;

    public ExpressionURISupplier(WorkflowValueResolver<String> expr) {
      this.expr = expr;
    }

    @Override
    public URI apply(WorkflowContext workflow, TaskContext task, WorkflowModel node) {
      return URI.create(expr.apply(workflow, task, node));
    }
  }
}
