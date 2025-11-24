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
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;
import io.serverlessworkflow.impl.executors.http.HttpExecutorBuilder;
import java.util.Map;

public class OpenAPIExecutorBuilder implements CallableTaskBuilder<CallOpenAPI> {

  private OpenAPIProcessor processor;
  private ExternalResource resource;
  private Map<String, Object> parameters;
  private HttpExecutorBuilder builder;

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallOpenAPI.class);
  }

  @Override
  public void init(
      CallOpenAPI task, WorkflowDefinition definition, WorkflowMutablePosition position) {
    OpenAPIArguments with = task.getWith();
    this.processor = new OpenAPIProcessor(with.getOperationId());
    this.resource = with.getDocument();
    this.parameters =
        with.getParameters() != null && with.getParameters().getAdditionalProperties() != null
            ? with.getParameters().getAdditionalProperties()
            : Map.of();
    this.builder =
        HttpExecutorBuilder.builder(definition)
            .withAuth(with.getAuthentication())
            .redirect(with.isRedirect());
  }

  @Override
  public CallableTask build() {
    return new OpenAPIExecutor(processor, resource, parameters, builder);
  }
}
