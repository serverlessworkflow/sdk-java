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
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.JQExpressionFactory;

public class DefaultTaskExecutorFactory implements TaskExecutorFactory {

  private final ExpressionFactory exprFactory;

  private static TaskExecutorFactory instance =
      new DefaultTaskExecutorFactory(JQExpressionFactory.get());

  public static TaskExecutorFactory get() {
    return instance;
  }

  public static TaskExecutorFactory get(ExpressionFactory factory) {
    return new DefaultTaskExecutorFactory(factory);
  }

  protected DefaultTaskExecutorFactory(ExpressionFactory exprFactory) {
    this.exprFactory = exprFactory;
  }

  public TaskExecutor<? extends TaskBase> getTaskExecutor(Task task) {
    if (task.getCallTask() != null) {
      CallTask callTask = task.getCallTask();
      if (callTask.getCallHTTP() != null) {
        return new HttpExecutor(callTask.getCallHTTP(), exprFactory);
      }
    }
    throw new UnsupportedOperationException(task.get().getClass().getName() + " not supported yet");
  }
}
