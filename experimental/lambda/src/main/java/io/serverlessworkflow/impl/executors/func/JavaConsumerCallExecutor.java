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
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.CallableTaskBuilder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class JavaConsumerCallExecutor<T>
    implements CallableTaskBuilder<CallJava.CallJavaConsumer<T>> {

  private Consumer<T> consumer;
  private Optional<Class<T>> inputClass;

  public void init(
      CallJava.CallJavaConsumer<T> task,
      WorkflowDefinition definition,
      WorkflowMutablePosition position) {
    consumer = task.consumer();
    inputClass = task.inputClass();
  }

  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    T typed = JavaFuncUtils.convertT(input, inputClass);
    consumer.accept(typed);
    return CompletableFuture.completedFuture(input);
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return CallJava.CallJavaConsumer.class.isAssignableFrom(clazz);
  }

  @Override
  public CallableTask build() {
    return this::apply;
  }
}
