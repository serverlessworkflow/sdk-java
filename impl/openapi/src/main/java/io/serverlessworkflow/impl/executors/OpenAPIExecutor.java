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
package io.serverlessworkflow.impl.executors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.EndpointUri;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Optional;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {
  private static final Client client = ClientBuilder.newClient();
  private TargetSupplier targetSupplier;

  @FunctionalInterface
  private interface TargetSupplier {
    WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node);
  }

  @Override
  public void init(CallOpenAPI task, WorkflowDefinition definition) {
    OpenAPIArguments args = task.getWith();
    this.targetSupplier = getTargetSupplier(args, definition.expressionFactory());
  }

  @Override
  public JsonNode apply(
      WorkflowContext workflowContext, TaskContext<CallOpenAPI> taskContext, JsonNode input) {

    WebTarget target = this.targetSupplier.apply(workflowContext, taskContext, input);

    System.out.println("target: " + target.getUri());

    return input;
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.isAssignableFrom(CallOpenAPI.class);
  }

  private static TargetSupplier getURISupplier(UriTemplate template, String operationId) {
    if (template.getLiteralUri() != null) {

      Optional<JsonNode> jsonNode =
          WorkflowUtils.classpathResourceToNode(template.getLiteralUri().toString());

      if (jsonNode.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid OpenAPI specification " + template.getLiteralUri().toString());
      }

      String host = OpenAPIReader.getHost(jsonNode.get());

      Optional<JsonNode> possibleOperation =
          OpenAPIReader.readOperation(jsonNode.get(), operationId);

      if (possibleOperation.isEmpty()) {
        throw new WorkflowException(
            WorkflowError.error(WorkflowError.RUNTIME_TYPE, 400)
                .title("Invalid OpenAPI Specification")
                .details("There is no operation ID " + operationId)
                .build());
      }

      return (w, t, n) -> client.target(host);
    } else if (template.getLiteralUriTemplate() != null) {
      return (w, t, n) ->
          client
              .target(template.getLiteralUriTemplate())
              .resolveTemplates(
                  JsonUtils.mapper().convertValue(n, new TypeReference<Map<String, Object>>() {}));
    }
    throw new IllegalArgumentException("Invalid uritemplate definition " + template);
  }

  private static class ExpressionURISupplier implements TargetSupplier {
    private Expression expr;

    public ExpressionURISupplier(Expression expr) {
      this.expr = expr;
    }

    @Override
    public WebTarget apply(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
      return client.target(expr.eval(workflow, task, node).asText());
    }
  }

  private static TargetSupplier getTargetSupplier(
      OpenAPIArguments args, ExpressionFactory expressionFactory) {

    Endpoint endpoint = args.getDocument().getEndpoint();
    String operationId = args.getOperationId();

    if (endpoint.getEndpointConfiguration() != null) {
      EndpointUri uri = endpoint.getEndpointConfiguration().getUri();
      if (uri.getLiteralEndpointURI() != null) {
        return getURISupplier(uri.getLiteralEndpointURI(), operationId);
      } else if (uri.getExpressionEndpointURI() != null) {
        return new ExpressionURISupplier(
            expressionFactory.getExpression(uri.getExpressionEndpointURI()));
      }
    } else if (endpoint.getRuntimeExpression() != null) {
      return new ExpressionURISupplier(
          expressionFactory.getExpression(endpoint.getRuntimeExpression()));
    } else if (endpoint.getUriTemplate() != null) {
      return getURISupplier(endpoint.getUriTemplate(), operationId);
    }
    throw new IllegalArgumentException("Invalid endpoint definition " + endpoint);
  }
}
