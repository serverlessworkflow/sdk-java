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
import io.serverlessworkflow.api.types.RunTaskConfiguration.ProcessReturnType;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.scripts.ScriptContext;
import io.serverlessworkflow.impl.scripts.ScriptRunner;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RunScriptExecutor implements CallableTask {

  private final Optional<WorkflowValueResolver<Map<String, Object>>> environmentExpr;
  private final Optional<WorkflowValueResolver<Map<String, Object>>> argumentExpr;
  private final WorkflowValueResolver<String> codeSupplier;
  private final boolean isAwait;
  private final RunTaskConfiguration.ProcessReturnType returnType;
  private final ScriptRunner taskRunner;

  public RunScriptExecutor(
      Optional<WorkflowValueResolver<Map<String, Object>>> environmentExpr,
      Optional<WorkflowValueResolver<Map<String, Object>>> argumentExpr,
      WorkflowValueResolver<String> codeSupplier,
      boolean isAwait,
      ProcessReturnType returnType,
      ScriptRunner taskRunner) {
    this.environmentExpr = environmentExpr;
    this.argumentExpr = argumentExpr;
    this.codeSupplier = codeSupplier;
    this.isAwait = isAwait;
    this.returnType = returnType;
    this.taskRunner = taskRunner;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    ScriptContext scriptContext =
        new ScriptContext(
            argumentExpr.map(m -> m.apply(workflowContext, taskContext, input)).orElse(Map.of()),
            environmentExpr.map(m -> m.apply(workflowContext, taskContext, input)).orElse(Map.of()),
            codeSupplier.apply(workflowContext, taskContext, input),
            returnType);
    if (isAwait) {
      return CompletableFuture.supplyAsync(
          () -> runScript(scriptContext, workflowContext, taskContext, input),
          workflowContext.definition().application().executorService());
    } else {
      workflowContext
          .definition()
          .application()
          .executorService()
          .submit(() -> runScript(scriptContext, workflowContext, taskContext, input));
      return CompletableFuture.completedFuture(input);
    }
  }

  private WorkflowModel runScript(
      ScriptContext scriptContext,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    try {
      return taskRunner.runScript(scriptContext, workflowContext, taskContext, input);
    } catch (Exception ex) {
      throw new WorkflowException(WorkflowError.runtime(taskContext, ex).build());
    }
  }
}
