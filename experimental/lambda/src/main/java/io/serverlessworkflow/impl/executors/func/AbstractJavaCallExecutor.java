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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.executors.CallableTask;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractJavaCallExecutor<T> implements CallableTask {

  protected final Optional<Class<T>> inputClass;

  protected AbstractJavaCallExecutor() {
    this(Optional.empty());
  }

  protected AbstractJavaCallExecutor(Optional<Class<T>> inputClass) {
    this.inputClass = inputClass;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    Object result = callJavaFunction(workflowContext, taskContext, model2Input(input));
    WorkflowModelFactory modelFactory = workflowContext.definition().application().modelFactory();
    return result instanceof CompletableFuture future
        ? future.thenApply(v -> output2Model(modelFactory, input, v))
        : CompletableFuture.completedFuture(output2Model(modelFactory, input, result));
  }

  protected abstract Object callJavaFunction(
      WorkflowContext workflowContext, TaskContext taskContext, T input);

  protected T model2Input(WorkflowModel model) {
    return JavaFuncUtils.convertT(model, inputClass);
  }

  protected Object convertResponse(Object obj) {
    return obj == null
        ? null
        : DataTypeConverterRegistry.get().find(obj.getClass()).map(c -> c.apply(obj)).orElse(obj);
  }

  protected WorkflowModel output2Model(
      WorkflowModelFactory modelFactory, WorkflowModel input, Object result) {
    return modelFactory.fromAny(input, convertResponse(result));
  }
}
