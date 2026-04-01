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

public abstract class AbstractJavaCallExecutor<T, V> implements CallableTask {

  protected final Optional<Class<T>> inputClass;
  private final Optional<Class<V>> outputClass;
  private final Optional<DataTypeConverter> typeConverter;
  private final boolean directCompletable;
  private final boolean convertedCompletable;

  protected AbstractJavaCallExecutor() {
    this(Optional.empty(), Optional.empty());
  }

  protected AbstractJavaCallExecutor(
      Optional<Class<T>> inputClass, Optional<Class<V>> outputClass) {
    this.inputClass = inputClass;
    this.outputClass = outputClass;
    this.typeConverter = outputClass.flatMap(DataTypeConverterRegistry.get()::find);
    this.directCompletable = outputClass.filter(c -> c.equals(CompletableFuture.class)).isPresent();
    this.convertedCompletable =
        typeConverter.filter(c -> c.targetType().equals(CompletableFuture.class)).isPresent();
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    WorkflowModelFactory modelFactory = workflowContext.definition().application().modelFactory();

    if (directCompletable) {
      return ((CompletableFuture<?>)
              callJavaFunction(workflowContext, taskContext, model2Input(input)))
          .thenApply(v -> output2Model(modelFactory, input, convertResponse(v)));
    } else if (convertedCompletable) {
      return ((CompletableFuture<?>)
              convertTypedResponse(
                  callJavaFunction(workflowContext, taskContext, model2Input(input))))
          .thenApply(v -> output2Model(modelFactory, input, convertResponse(v)));
    } else if (outputClass.isPresent()) {
      return CompletableFuture.supplyAsync(
              () -> callJavaFunction(workflowContext, taskContext, model2Input(input)),
              workflowContext.definition().application().executorService())
          .thenApply(v -> output2Model(modelFactory, input, convertTypedResponse(v)));
    } else {
      Object result =
          convertResponse(callJavaFunction(workflowContext, taskContext, model2Input(input)));
      return result instanceof CompletableFuture future
          ? future.thenApply(v -> output2Model(modelFactory, input, convertResponse(v)))
          : CompletableFuture.completedFuture(output2Model(modelFactory, input, result));
    }
  }

  protected abstract V callJavaFunction(
      WorkflowContext workflowContext, TaskContext taskContext, T input);

  protected T model2Input(WorkflowModel model) {
    return JavaFuncUtils.convertT(model, inputClass);
  }

  protected Object convertTypedResponse(V obj) {
    return obj == null ? null : typeConverter.map(c -> c.apply(obj)).orElse(obj);
  }

  protected Object convertResponse(Object obj) {
    return obj == null || obj instanceof CompletableFuture
        ? obj
        : DataTypeConverterRegistry.get().find(obj.getClass()).map(c -> c.apply(obj)).orElse(obj);
  }

  protected WorkflowModel output2Model(
      WorkflowModelFactory modelFactory, WorkflowModel input, Object result) {
    return modelFactory.fromAny(input, result);
  }
}
