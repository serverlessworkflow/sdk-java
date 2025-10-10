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

import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.http.TargetSupplier;
import jakarta.ws.rs.client.WebTarget;

class OperationDefinitionSupplier {

  private final WorkflowApplication application;
  private final CallOpenAPI task;

  OperationDefinitionSupplier(WorkflowApplication application, CallOpenAPI task) {
    this.task = task;
    this.application = application;
  }

  OperationDefinition get(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    TargetSupplier targetSupplier =
        getTargetSupplier(
            task.getWith().getDocument().getEndpoint(), application.expressionFactory());

    String operationId = task.getWith().getOperationId();
    WebTarget webTarget = targetSupplier.apply(workflowContext, taskContext, input);
    OpenAPIProcessor processor = new OpenAPIProcessor(operationId, webTarget.getUri());
    return processor.parse();
  }
}
