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
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FlexibleProcessExecutor implements CallableTask {

  private final FlexibleProcess flexibleProcess;
  private Supplier<Map<Activity, TaskExecutor<?>>> executors;

  public FlexibleProcessExecutor(
      FlexibleProcess flexibleProcess, Supplier<Map<Activity, TaskExecutor<?>>> executors) {
    this.flexibleProcess = flexibleProcess;
    this.executors = executors;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    FlexibleProcessManager manager = new FlexibleProcessManager(flexibleProcess, executors.get());
    return manager.run(workflowContext, Optional.of(taskContext), input);
  }
}
