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
import io.serverlessworkflow.api.types.func.CallJava.CallJavaLoopFunctionIndex;
import io.serverlessworkflow.api.types.func.LoopFunctionIndex;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;

public class JavaLoopFunctionIndexCallExecutorBuilder
    implements CallableTaskBuilder<CallJava.CallJavaLoopFunctionIndex> {

  private LoopFunctionIndex function;
  private String varName;
  private String indexName;

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
    return new JavaLoopFunctionIndexCallExecutor<>(function, varName, indexName);
  }
}
