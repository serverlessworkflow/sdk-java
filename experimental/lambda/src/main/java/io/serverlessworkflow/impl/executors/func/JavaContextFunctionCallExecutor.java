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

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.CallableTask;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JavaContextFunctionCallExecutor<T, V>
    implements CallableTask<CallJava.CallJavaContextFunction<T, V>> {

  private JavaContextFunction<T, V> function;
  private Optional<Class<T>> inputClass;

  @Override
  public void init(CallJava.CallJavaContextFunction<T, V> task, WorkflowDefinition definition) {
    this.function = task.function();
    this.inputClass = task.inputClass();
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    return CompletableFuture.completedFuture(
        workflowContext
            .definition()
            .application()
            .modelFactory()
            .fromAny(
                input, function.apply(JavaFuncUtils.convertT(input, inputClass), workflowContext)));
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return CallJava.CallJavaContextFunction.class.isAssignableFrom(clazz);
  }
}
