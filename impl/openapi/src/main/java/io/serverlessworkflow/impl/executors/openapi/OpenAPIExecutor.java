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

import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.http.HttpExecutor;
import io.serverlessworkflow.impl.executors.http.HttpExecutor.HttpExecutorBuilder;
import io.serverlessworkflow.impl.resources.ResourceLoaderUtils;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpenAPIExecutor implements CallableTask<CallOpenAPI> {

  private OpenAPIProcessor processor;
  private ExternalResource resource;
  private Map<String, Object> parameters;
  private HttpExecutorBuilder builder;

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  @Override
  public void init(CallOpenAPI task, WorkflowDefinition definition) {
    OpenAPIArguments with = task.getWith();
    this.processor = new OpenAPIProcessor(with.getOperationId());
    this.resource = with.getDocument();
    this.parameters =
        with.getParameters() != null && with.getParameters().getAdditionalProperties() != null
            ? with.getParameters().getAdditionalProperties()
            : Map.of();
    this.builder =
        HttpExecutor.builder(definition)
            .withAuth(with.getAuthentication())
            .redirect(with.isRedirect());
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    // In the same workflow, access to an already cached document
    final OperationDefinition operationDefinition =
        processor.parse(
            workflowContext
                .definition()
                .resourceLoader()
                .load(
                    resource,
                    ResourceLoaderUtils::readString,
                    workflowContext,
                    taskContext,
                    input));

    fillHttpBuilder(workflowContext.definition().application(), operationDefinition);
    // One executor per operation, even if the document is the same
    // Me may refactor this even further to reuse the same executor (since the base URI is the same,
    // but the path differs, although some use cases may require different client configurations for
    // different paths...)
    Collection<HttpExecutor> executors =
        operationDefinition.getServers().stream().map(s -> builder.build(s)).toList();

    Iterator<HttpExecutor> iter = executors.iterator();
    if (!iter.hasNext()) {
      throw new IllegalArgumentException(
          "List of servers is empty for schema " + resource.getName());
    }
    CompletableFuture<WorkflowModel> future =
        iter.next().apply(workflowContext, taskContext, input);
    while (iter.hasNext()) {
      future.exceptionallyCompose(i -> iter.next().apply(workflowContext, taskContext, input));
    }
    return future;
  }

  private void fillHttpBuilder(WorkflowApplication application, OperationDefinition operation) {
    Map<String, Object> headersMap = new HashMap<>();
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> pathParameters = new HashMap<>();

    Map<String, Object> bodyParameters = new HashMap<>(parameters);
    for (Parameter parameter : operation.getParameters()) {
      switch (parameter.getIn()) {
        case "header":
          param(parameter.getName(), bodyParameters, headersMap);
          break;
        case "path":
          param(parameter.getName(), bodyParameters, pathParameters);
          break;
        case "query":
          param(parameter.getName(), bodyParameters, queryMap);
          break;
      }
    }

    validateRequiredParameters(operation, headersMap, queryMap, pathParameters);

    builder
        .withMethod(operation.getMethod())
        .withPath(new OperationPathResolver(operation.getPath(), application, pathParameters))
        .withBody(bodyParameters)
        .withQueryMap(queryMap)
        .withHeaders(headersMap);
  }

  private void param(String name, Map<String, Object> origMap, Map<String, Object> collectorMap) {
    Object value = origMap.remove(name);
    if (value != null) {
      collectorMap.put(name, value);
    }
  }

  private void validateRequiredParameters(
      OperationDefinition operation,
      Map<String, Object> headersMap,
      Map<String, Object> queryMap,
      Map<String, Object> pathParameters) {

    StringBuilder missing = new StringBuilder();

    for (Parameter parameter : operation.getParameters()) {
      if (!Boolean.TRUE.equals(parameter.getRequired())) {
        continue;
      }

      String in = parameter.getIn();
      String name = parameter.getName();

      Map<String, Object> targetMap =
          switch (in) {
            case "header" -> headersMap;
            case "path" -> pathParameters;
            case "query" -> queryMap;
            default -> null;
          };

      if (targetMap == null) {
        // We don't currently handle other "in" locations here (e.g., cookie).
        // Treat as "not validated" instead of failing.
        continue;
      }

      boolean present = targetMap.containsKey(name);

      if (!present) {
        // Try to satisfy the requirement using the OpenAPI default, if any
        Schema<?> schema = parameter.getSchema();
        Object defaultValue = schema != null ? schema.getDefault() : null;

        if (defaultValue != null) {
          targetMap.put(name, defaultValue);
          present = true;
        }
      }

      if (!present) {
        if (!missing.isEmpty()) {
          missing.append(", ");
        }
        missing.append(in).append(" parameter '").append(name).append("'");
      }
    }

    if (!missing.isEmpty()) {
      String operationId =
          operation.getOperation().getOperationId() != null
              ? operation.getOperation().getOperationId()
              : "<unknown>";
      throw new IllegalArgumentException(
          "Missing required OpenAPI parameters for operation '" + operationId + "': " + missing);
    }
  }
}
