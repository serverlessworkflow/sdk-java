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

import static io.serverlessworkflow.impl.executors.func.JavaFuncUtils.predObject;

import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.Until;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.UntilPredicate;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.executors.ListenExecutor.ListenExecutorBuilder;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import io.serverlessworkflow.impl.resources.ResourceLoader;

public class JavaListenExecutorBuilder extends ListenExecutorBuilder {

  protected JavaListenExecutorBuilder(
      WorkflowMutablePosition position,
      ListenTask task,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    super(position, task, workflow, application, resourceLoader);
  }

  @Override
  protected WorkflowPredicate buildUntilPredicate(Until until) {
    return until instanceof UntilPredicate untilPred && untilPred.predicate() != null
        ? application
            .expressionFactory()
            .buildPredicate(
                ExpressionDescriptor.object(
                    predObject(untilPred.predicate(), untilPred.predicateClass())))
        : super.buildUntilPredicate(until);
  }
}
