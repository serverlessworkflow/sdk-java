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

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

public class DefaultTaskExecutorFactory implements TaskExecutorFactory {

  private static TaskExecutorFactory instance = new DefaultTaskExecutorFactory();

  public static TaskExecutorFactory get() {
    return instance;
  }

  protected DefaultTaskExecutorFactory() {}

  private ServiceLoader<CallableTask> callTasks = ServiceLoader.load(CallableTask.class);

  public TaskExecutor<? extends TaskBase> getTaskExecutor(
      Task task, WorkflowDefinition definition) {
    if (task.getCallTask() != null) {
      CallTask callTask = task.getCallTask();
      if (callTask.getCallHTTP() != null) {
        return new CallTaskExecutor<>(
            callTask.getCallHTTP(), definition, findCallTask(CallHTTP.class));
      }
    } else if (task.getSwitchTask() != null) {
      return new SwitchExecutor(task.getSwitchTask(), definition);
    } else if (task.getDoTask() != null) {
      return new DoExecutor(task.getDoTask(), definition);
    } else if (task.getSetTask() != null) {
      return new SetExecutor(task.getSetTask(), definition);
    }
    throw new UnsupportedOperationException(task.get().getClass().getName() + " not supported yet");
  }

  @SuppressWarnings("unchecked")
  private <T extends TaskBase> CallableTask<T> findCallTask(Class<T> clazz) {
    return (CallableTask<T>)
        callTasks.stream()
            .map(Provider::get)
            .filter(s -> s.accept(clazz))
            .findAny()
            .orElseThrow(
                () -> new UnsupportedOperationException(clazz.getName() + " not supported yet"));
  }
}
