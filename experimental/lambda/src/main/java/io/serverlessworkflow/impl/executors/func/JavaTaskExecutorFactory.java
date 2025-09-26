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
package io.serverlessworkflow.impl.executors.func;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutorBuilder;

public class JavaTaskExecutorFactory extends DefaultTaskExecutorFactory {

  public TaskExecutorBuilder<? extends TaskBase> getTaskExecutor(
      WorkflowMutablePosition position, Task task, WorkflowDefinition definition) {
    if (task.getForTask() != null) {
      return new JavaForExecutorBuilder(position, task.getForTask(), definition);
    } else if (task.getSwitchTask() != null) {
      return new JavaSwitchExecutorBuilder(position, task.getSwitchTask(), definition);
    } else if (task.getListenTask() != null) {
      return new JavaListenExecutorBuilder(position, task.getListenTask(), definition);
    } else {
      return super.getTaskExecutor(position, task, definition);
    }
  }
}
