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
    this.parameters = with.getParameters().getAdditionalProperties();
    this.builder =
        HttpExecutor.builder(definition)
            .withAuth(with.getAuthentication())
            .redirect(with.isRedirect());
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    Collection<HttpExecutor> executors =
        workflowContext
            .definition()
            .resourceLoader()
            .<Collection<HttpExecutor>>load(
                resource,
                r -> {
                  OperationDefinition o = processor.parse(ResourceLoaderUtils.readString(r));
                  fillHttpBuilder(workflowContext.definition().application(), o);
                  return o.getServers().stream().map(s -> builder.build(s)).toList();
                },
                workflowContext,
                taskContext,
                input);

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
    Map<String, Object> headersMap = new HashMap<String, Object>();
    Map<String, Object> queryMap = new HashMap<String, Object>();
    Map<String, Object> pathParameters = new HashMap<String, Object>();

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
}
