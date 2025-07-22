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

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.executors.CallTaskExecutor.CallTaskExecutorBuilder;
import io.serverlessworkflow.impl.executors.DoExecutor.DoExecutorBuilder;
import io.serverlessworkflow.impl.executors.EmitExecutor.EmitExecutorBuilder;
import io.serverlessworkflow.impl.executors.ForExecutor.ForExecutorBuilder;
import io.serverlessworkflow.impl.executors.ForkExecutor.ForkExecutorBuilder;
import io.serverlessworkflow.impl.executors.ListenExecutor.ListenExecutorBuilder;
import io.serverlessworkflow.impl.executors.RaiseExecutor.RaiseExecutorBuilder;
import io.serverlessworkflow.impl.executors.SetExecutor.SetExecutorBuilder;
import io.serverlessworkflow.impl.executors.SwitchExecutor.SwitchExecutorBuilder;
import io.serverlessworkflow.impl.executors.TryExecutor.TryExecutorBuilder;
import io.serverlessworkflow.impl.executors.WaitExecutor.WaitExecutorBuilder;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

public class DefaultTaskExecutorFactory implements TaskExecutorFactory {

  private static TaskExecutorFactory instance = new DefaultTaskExecutorFactory();

  public static TaskExecutorFactory get() {
    return instance;
  }

  protected DefaultTaskExecutorFactory() {}

  private ServiceLoader<CallableTask> callTasks = ServiceLoader.load(CallableTask.class);

  @Override
  public TaskExecutorBuilder<? extends TaskBase> getTaskExecutor(
      WorkflowPosition position,
      Task task,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    if (task.getCallTask() != null) {
      CallTask callTask = task.getCallTask();
      TaskBase taskBase = (TaskBase) callTask.get();
      if (taskBase != null) {
        return new CallTaskExecutorBuilder(
            position,
            taskBase,
            workflow,
            application,
            resourceLoader,
            findCallTask(taskBase.getClass()));
      }
    } else if (task.getSwitchTask() != null) {
      return new SwitchExecutorBuilder(
          position, task.getSwitchTask(), workflow, application, resourceLoader);
    } else if (task.getDoTask() != null) {
      return new DoExecutorBuilder(
          position, task.getDoTask(), workflow, application, resourceLoader);
    } else if (task.getSetTask() != null) {
      return new SetExecutorBuilder(
          position, task.getSetTask(), workflow, application, resourceLoader);
    } else if (task.getForTask() != null) {
      return new ForExecutorBuilder(
          position, task.getForTask(), workflow, application, resourceLoader);
    } else if (task.getRaiseTask() != null) {
      return new RaiseExecutorBuilder(
          position, task.getRaiseTask(), workflow, application, resourceLoader);
    } else if (task.getTryTask() != null) {
      return new TryExecutorBuilder(
          position, task.getTryTask(), workflow, application, resourceLoader);
    } else if (task.getForkTask() != null) {
      return new ForkExecutorBuilder(
          position, task.getForkTask(), workflow, application, resourceLoader);
    } else if (task.getWaitTask() != null) {
      return new WaitExecutorBuilder(
          position, task.getWaitTask(), workflow, application, resourceLoader);
    } else if (task.getListenTask() != null) {
      return new ListenExecutorBuilder(
          position, task.getListenTask(), workflow, application, resourceLoader);
    } else if (task.getEmitTask() != null) {
      return new EmitExecutorBuilder(
          position, task.getEmitTask(), workflow, application, resourceLoader);
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
