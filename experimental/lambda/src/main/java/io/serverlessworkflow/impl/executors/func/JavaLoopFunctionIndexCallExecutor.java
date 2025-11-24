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

import static io.serverlessworkflow.impl.executors.func.JavaFuncUtils.safeObject;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallJava.CallJavaLoopFunctionIndex;
import io.serverlessworkflow.api.types.func.LoopFunctionIndex;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;
import java.util.concurrent.CompletableFuture;

public class JavaLoopFunctionIndexCallExecutor
    implements CallableTaskBuilder<CallJava.CallJavaLoopFunctionIndex> {

  private LoopFunctionIndex function;
  private String varName;
  private String indexName;

  private CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    WorkflowModelFactory modelFactory = workflowContext.definition().application().modelFactory();

    return CompletableFuture.completedFuture(
        modelFactory.fromAny(
            input,
            function.apply(
                input.asJavaObject(),
                safeObject(taskContext.variables().get(varName)),
                (Integer) safeObject(taskContext.variables().get(indexName)))));
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return CallJava.CallJavaLoopFunctionIndex.class.isAssignableFrom(clazz);
  }

  @Override
  public void init(
      CallJavaLoopFunctionIndex task,
      WorkflowDefinition definition,
      WorkflowMutablePosition position) {
    function = task.function();
    varName = task.varName();
    indexName = task.indexName();
  }

  @Override
  public CallableTask build() {
    return this::apply;
  }
}
