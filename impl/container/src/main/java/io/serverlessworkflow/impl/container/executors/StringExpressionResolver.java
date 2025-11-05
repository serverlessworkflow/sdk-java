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
package io.serverlessworkflow.impl.container.executors;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import java.util.function.Function;

class StringExpressionResolver implements Function<String, String> {

  private final WorkflowContext workflowContext;
  private final TaskContext taskContext;
  private final WorkflowModel input;

  StringExpressionResolver(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    this.workflowContext = workflowContext;
    this.taskContext = taskContext;
    this.input = input;
  }

  public String apply(String expression) {
    if (ExpressionUtils.isExpr(expression)) {
      return WorkflowUtils.buildStringResolver(
              workflowContext.definition().application(),
              expression,
              taskContext.input().asJavaObject())
          .apply(workflowContext, taskContext, input);
    }
    return expression;
  }
}
