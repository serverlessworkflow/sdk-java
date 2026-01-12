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

import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.Catalog;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Use;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.resources.ExternalResourceHandler;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class CallFunctionExecutorBuilder implements CallableTaskBuilder<CallFunction> {

  private TaskExecutorBuilder<? extends TaskBase> executorBuilder;
  private WorkflowValueResolver<Map<String, Object>> args;

  @Override
  public void init(
      CallFunction task, WorkflowDefinition definition, WorkflowMutablePosition position) {
    String functionName = task.getCall();
    Use use = definition.workflow().getUse();
    int indexOf = functionName.indexOf('@');
    Task function = null;
    if (indexOf > 0) {
      // Catalog function
      URI catalogEndpoint;
      String catalogName = functionName.substring(indexOf + 1);
      ResourceLoader loader = definition.resourceLoader();
      if (catalogName.equalsIgnoreCase("default")) {
        catalogEndpoint = definition.application().defaultCatalogURI();
      } else {
        if (use == null || use.getCatalogs() == null) {
          throw new IllegalStateException(
              "Using catalog " + catalogName + ", but there is not catalog definition");
        }
        Catalog catalog = use.getCatalogs().getAdditionalProperties().get(catalogName);
        if (catalog == null) {
          throw new IllegalStateException(
              "Catalog "
                  + catalogName
                  + " is not included in Catalog dictionary: "
                  + use.getCatalogs().getAdditionalProperties());
        }
        catalogEndpoint = loader.uri(catalog.getEndpoint());
      }
      function =
          definition
              .resourceLoader()
              .loadURI(
                  WorkflowUtils.concatURI(
                      catalogEndpoint, pathFromFunctionName(functionName.substring(0, indexOf))),
                  h -> from(definition, h));
    } else if (use != null && use.getFunctions() != null) {
      // search for inline function definition
      function = use.getFunctions().getAdditionalProperties().get(functionName);
    }
    if (function == null) {
      // try to load function if function name is an uri
      function =
          definition.resourceLoader().loadURI(URI.create(functionName), h -> from(definition, h));
    }
    executorBuilder =
        definition.application().taskFactory().getTaskExecutor(position, function, definition);
    FunctionArguments functionArgs = task.getWith();
    args =
        functionArgs != null
            ? WorkflowUtils.buildMapResolver(
                definition.application(), functionArgs.getAdditionalProperties())
            : (w, t, m) -> Map.of();
  }

  private String pathFromFunctionName(String functionName) {
    int sep = functionName.indexOf(":");
    if (sep < 0) {
      throw new IllegalArgumentException(
          "Invalid function name "
              + functionName
              + ". It has to be of the format <function name>:<function version>");
    }
    StringBuilder sb = new StringBuilder(functionName);
    sb.setCharAt(sep, '/');
    sb.insert(0, "main/functions/");
    sb.append("/function.yaml");
    return sb.toString();
  }

  private Task from(WorkflowDefinition definition, ExternalResourceHandler handler) {
    return definition
        .application()
        .functionReader()
        .map(v -> v.apply(handler))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No converter from external resource to function found. Make sure a dependency that includes an implementation of FunctionReader is included"));
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallFunction.class);
  }

  @Override
  public CallableTask build() {
    TaskExecutor<? extends TaskBase> executor = executorBuilder.build();
    return (w, t, m) ->
        executor
            .apply(
                w,
                Optional.of(t),
                w.definition().application().modelFactory().fromAny(args.apply(w, t, m)))
            .thenApply(o -> o.output());
  }
}
