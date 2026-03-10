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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.RunTask;
import io.serverlessworkflow.api.types.RunTaskConfigurationUnion;
import io.serverlessworkflow.api.types.RunWorkflow;
import io.serverlessworkflow.api.types.SubflowConfiguration;
import io.serverlessworkflow.fluent.spec.spi.WorkflowTaskFluent;

public class WorkflowTaskBuilder extends TaskBaseBuilder<WorkflowTaskBuilder>
    implements WorkflowTaskFluent<WorkflowTaskBuilder> {

  private final RunTask task;
  private final RunWorkflow configuration;
  private final SubflowConfiguration workflow;

  WorkflowTaskBuilder() {
    this.task = new RunTask();
    this.configuration = new RunWorkflow();
    this.workflow = new SubflowConfiguration();
    this.configuration.setWorkflow(this.workflow);
    this.task.setRun(new RunTaskConfigurationUnion().withRunWorkflow(this.configuration));
    this.setTask(task);
  }

  @Override
  public WorkflowTaskBuilder self() {
    return this;
  }

  public RunWorkflow config() {
    return this.configuration;
  }

  public SubflowConfiguration workflow() {
    return this.workflow;
  }

  public RunTask build() {
    return this.task;
  }
}
