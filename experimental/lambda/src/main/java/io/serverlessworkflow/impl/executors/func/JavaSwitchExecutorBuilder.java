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

import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.func.SwitchCaseFunction;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowPredicate;
import io.serverlessworkflow.impl.executors.SwitchExecutor.SwitchExecutorBuilder;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import java.util.Optional;

public class JavaSwitchExecutorBuilder extends SwitchExecutorBuilder {

  protected JavaSwitchExecutorBuilder(
      WorkflowMutablePosition position, SwitchTask task, WorkflowDefinition definition) {
    super(position, task, definition);
  }

  @Override
  protected Optional<WorkflowPredicate> buildFilter(SwitchCase switchCase) {
    return switchCase instanceof SwitchCaseFunction function
        ? Optional.of(
            application
                .expressionFactory()
                .buildPredicate(
                    ExpressionDescriptor.object(
                        predObject(function.predicate(), function.predicateClass()))))
        : super.buildFilter(switchCase);
  }
}
