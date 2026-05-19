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
package io.serverlessworkflow.impl.events;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationPredicate {

  private static final Logger logger = LoggerFactory.getLogger(CorrelationPredicate.class);

  private final String key;
  private final WorkflowValueResolver<Object> fromResolver;
  private final WorkflowValueResolver<Object> expectResolver;

  private CorrelationPredicate(
      String key,
      WorkflowValueResolver<Object> fromResolver,
      WorkflowValueResolver<Object> expectResolver) {
    this.key = key;
    this.fromResolver = fromResolver;
    this.expectResolver = expectResolver;
  }

  public static CorrelationPredicate from(
      String key, io.serverlessworkflow.api.types.CorrelateProperty prop, WorkflowApplication app) {
    WorkflowValueResolver<Object> fromResolver =
        app.expressionFactory().resolveValue(ExpressionDescriptor.from(prop.getFrom()));
    WorkflowValueResolver<Object> expectResolver =
        prop.getExpect() != null
            ? app.expressionFactory().resolveValue(ExpressionDescriptor.from(prop.getExpect()))
            : null;
    return new CorrelationPredicate(key, fromResolver, expectResolver);
  }

  public boolean test(WorkflowModel eventModel, WorkflowContext workflow, TaskContext task) {
    Object eventValue = fromResolver.apply(workflow, task, eventModel);
    if (eventValue == null) {
      logger.debug("Correlation key '{}': from expression returned null", key);
      return false;
    }

    if (expectResolver == null) {
      logger.debug(
          "Correlation key '{}': no expect expression, accepting event value '{}'",
          key,
          eventValue);
      return true;
    }

    Object expectedValue = expectResolver.apply(workflow, task, task.input());
    boolean result = Objects.equals(eventValue, expectedValue);
    logger.debug(
        "Correlation key '{}': eventValue='{}', expectedValue='{}', match={}",
        key,
        eventValue,
        expectedValue,
        result);
    return result;
  }

  public String key() {
    return key;
  }
}
