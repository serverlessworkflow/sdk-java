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

import io.serverlessworkflow.api.types.func.LoopFunction;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;

public class JavaLoopFunctionCallExecutor<T, V, R> extends AbstractJavaCallExecutor<T> {

  private final LoopFunction<T, V, R> function;
  private final String varName;

  public JavaLoopFunctionCallExecutor(LoopFunction<T, V, R> function, String varName) {
    this.function = function;
    this.varName = varName;
  }

  @Override
  protected Object callJavaFunction(
      WorkflowContext workflowContext, TaskContext taskContext, T input) {
    return function.apply(input, (V) safeObject(taskContext.variables().get(varName)));
  }
}
