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

import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.api.types.WithOpenAPIParameters;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {

  private static final Client client = ClientBuilder.newClient();
  private WebTargetSupplier webTargetSupplier;
  private RequestSupplier requestSupplier;
  private final OpenAPIModelConverter converter = new OpenAPIModelConverter() {};

  @FunctionalInterface
  private interface WebTargetSupplier {
    WebTarget apply();
  }

  @FunctionalInterface
  private interface RequestSupplier {
    WorkflowModel apply(
        Invocation.Builder request, WorkflowContext workflow, TaskContext task, WorkflowModel node);
  }

  @Override
  public void init(
      CallOpenAPI task, Workflow workflow, WorkflowApplication application, ResourceLoader loader) {
    OpenAPIArguments args = task.getWith();
    WithOpenAPIParameters withParams = args.getParameters();

    URI uri = getOpenAPIDocumentURI(args.getDocument().getEndpoint().getUriTemplate());

    OpenAPIV3Parser apiv3Parser = new OpenAPIV3Parser();

    OpenAPI openAPI = apiv3Parser.read(uri.toString());

    OpenAPIOperationContext ctx = generateContext(openAPI, args, uri);

    this.webTargetSupplier =
        () -> {
          final AtomicReference<WebTarget> webTarget =
              new AtomicReference<>(
                  client
                      .target(openAPI.getServers().get(0).getUrl())
                      .path(ctx.buildPath(withParams.getAdditionalProperties())));

          MultivaluedMap<String, Object> queryParams =
              ctx.buildQuery(withParams.getAdditionalProperties());
          queryParams.forEach(
              (key, value) -> {
                for (Object o : value) {
                  webTarget.set(webTarget.get().queryParam(key, o));
                }
              });

          return webTarget.get();
        };

    this.requestSupplier =
        (request, w, taskContext, node) -> {
          Object response = request.method(ctx.httpMethodName(), node.objectClass());
          return converter.toModel(application.modelFactory(), node, response);
        };
  }

  private static OpenAPIOperationContext generateContext(
      OpenAPI openAPI, OpenAPIArguments args, URI uri) {
    return openAPI.getPaths().entrySet().stream()
        .flatMap(
            pathEntry ->
                pathEntry.getValue().readOperationsMap().entrySet().stream()
                    .map(
                        operationEntry ->
                            new OpenAPIOperationContext(
                                operationEntry.getValue().getOperationId(),
                                pathEntry.getKey(),
                                operationEntry.getKey(),
                                operationEntry.getValue())))
        .filter(c -> c.operationId().equals(args.getOperationId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Operation with id "
                        + args.getOperationId()
                        + " not found in OpenAPI document "
                        + uri));
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    return CompletableFuture.supplyAsync(
        () -> {
          WebTarget target = webTargetSupplier.apply();
          Invocation.Builder request = target.request();
          return requestSupplier.apply(request, workflowContext, taskContext, input);
        },
        workflowContext.definition().application().executorService());
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  private static URI getOpenAPIDocumentURI(UriTemplate template) {
    if (template.getLiteralUri() != null) {
      return template.getLiteralUri();
    } else if (template.getLiteralUriTemplate() != null) {
      // TODO: Support
      // https://github.com/serverlessworkflow/specification/blob/main/dsl-reference.md#uri-template
      throw new UnsupportedOperationException(
          "URI templates with parameters are not supported yet");
    }
    throw new IllegalArgumentException("Invalid UriTemplate definition " + template);
  }
}
