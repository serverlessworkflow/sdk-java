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

import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.api.types.RunWorkflow;
import io.serverlessworkflow.api.types.SubflowConfiguration;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowDefinitionId;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RunWorkflowExecutor implements RunnableTask<RunWorkflow> {

  private WorkflowDefinitionId workflowDefinitionId;
  private Map<String, Object> additionalParameters;

  public void init(RunWorkflow taskConfiguration, WorkflowDefinition definition) {
    SubflowConfiguration workflowConfig = taskConfiguration.getWorkflow();
    this.workflowDefinitionId =
        new WorkflowDefinitionId(
            workflowConfig.getNamespace(), workflowConfig.getName(), workflowConfig.getVersion());
    this.additionalParameters = workflowConfig.getInput().getAdditionalProperties();
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    WorkflowDefinition definition =
        workflowContext.definition().application().workflowDefinitions().get(workflowDefinitionId);
    if (definition != null) {
      //  TODO add additional parameters
      return definition.instance(input).start();
    } else {
      throw new IllegalArgumentException(
          "Workflow definition for " + workflowDefinitionId + " has not been found");
    }
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunWorkflow.class.equals(clazz);
  }
}
