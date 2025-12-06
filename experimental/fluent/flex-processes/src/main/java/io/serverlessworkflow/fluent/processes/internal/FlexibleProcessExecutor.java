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

import io.serverlessworkflow.fluent.processes.Activity;
import io.serverlessworkflow.fluent.processes.FlexibleProcess;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorBuilder;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FlexibleProcessExecutor implements CallableTask {

  private final WorkflowDefinition definition;
  private final FlexibleProcess flexibleProcess;

  public FlexibleProcessExecutor(FlexibleProcess flexibleProcess, WorkflowDefinition definition) {
    this.flexibleProcess = flexibleProcess;
    this.definition = definition;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    return CompletableFuture.supplyAsync(
        () -> {
          WorkflowMutablePosition position = definition.application().positionFactory().get();
          position.addProperty(UUID.randomUUID().toString());
          TaskExecutorFactory factory = definition.application().taskFactory();
          Map<Activity, TaskExecutor<?>> executors = new HashMap<>();

          for (Activity activity : flexibleProcess.getActivities()) {
            TaskExecutorBuilder<?> builder =
                factory.getTaskExecutor(position, activity.getTask(), definition);
            TaskExecutor<?> executor = builder.build();
            executors.put(activity, executor);
          }

          FlexibleProcessManager manager = new FlexibleProcessManager(flexibleProcess, executors);
          manager.run(workflowContext, Optional.of(taskContext), input);
          return input;
        });
  }
}
