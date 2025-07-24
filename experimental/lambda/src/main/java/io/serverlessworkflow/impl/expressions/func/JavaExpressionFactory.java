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
package io.serverlessworkflow.impl.expressions.func;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskMetadata;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class JavaExpressionFactory implements ExpressionFactory {

  public static final String IF_PREDICATE = "if_predicate";
  private final WorkflowModelFactory modelFactory = new JavaModelFactory();
  private final Expression dummyExpression =
      new Expression() {
        @Override
        public WorkflowModel eval(
            WorkflowContext workflowContext, TaskContext context, WorkflowModel model) {
          return model;
        }
      };

  @Override
  public Expression buildExpression(String expression) {
    return dummyExpression;
  }

  @Override
  public WorkflowFilter buildFilter(String expr, Object value) {
    if (value instanceof Function func) {
      return (w, t, n) -> modelFactory.fromAny(func.apply(n.asJavaObject()));
    } else if (value instanceof Predicate pred) {
      return fromPredicate(pred);
    } else if (value instanceof BiPredicate pred) {
      return (w, t, n) -> modelFactory.from(pred.test(w, t));
    } else if (value instanceof BiFunction func) {
      return (w, t, n) -> modelFactory.fromAny(func.apply(w, t));
    } else if (value instanceof WorkflowFilter filter) {
      return filter;
    } else {
      return (w, t, n) -> modelFactory.fromAny(value);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private WorkflowFilter fromPredicate(Predicate pred) {
    return (w, t, n) -> modelFactory.from(pred.test(n.asJavaObject()));
  }

  @Override
  public Optional<WorkflowFilter> buildIfFilter(TaskBase task) {
    TaskMetadata metadata = task.getMetadata();
    return metadata != null
            && metadata.getAdditionalProperties().get(IF_PREDICATE) instanceof Predicate pred
        ? Optional.of(fromPredicate(pred))
        : ExpressionFactory.super.buildIfFilter(task);
  }

  @Override
  public WorkflowModelFactory modelFactory() {
    return modelFactory;
  }
}
