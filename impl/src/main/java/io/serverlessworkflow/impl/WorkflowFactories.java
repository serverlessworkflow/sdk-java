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
package io.serverlessworkflow.impl;

import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.resources.ResourceLoader;

public class WorkflowFactories {

  private final TaskExecutorFactory taskFactory;
  private final ResourceLoader resourceLoader;
  private final ExpressionFactory expressionFactory;
  private final SchemaValidatorFactory validatorFactory;

  public WorkflowFactories(
      TaskExecutorFactory taskFactory,
      ResourceLoader resourceLoader,
      ExpressionFactory expressionFactory,
      SchemaValidatorFactory validatorFactory) {
    this.taskFactory = taskFactory;
    this.resourceLoader = resourceLoader;
    this.expressionFactory = expressionFactory;
    this.validatorFactory = validatorFactory;
  }

  public TaskExecutorFactory getTaskFactory() {
    return taskFactory;
  }

  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  public ExpressionFactory getExpressionFactory() {
    return expressionFactory;
  }

  public SchemaValidatorFactory getValidatorFactory() {
    return validatorFactory;
  }
}
