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
package io.serverlessworkflow.fluent.processes.internal;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.fluent.processes.FlexibleProcess;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;

public class FlexibleProcessExecutorBuilder implements CallableTaskBuilder<FlexibleProcess> {

  private FlexibleProcess task;
  private WorkflowDefinition definition;

  @Override
  public void init(
      FlexibleProcess task, WorkflowDefinition definition, WorkflowMutablePosition position) {
    this.task = task;
    this.definition = definition;
  }

  @Override
  public CallableTask build() {
    return new FlexibleProcessExecutor(task, definition);
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(FlexibleProcess.class);
  }
}
