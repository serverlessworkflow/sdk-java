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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.JQExpressionFactory;
import io.serverlessworkflow.impl.jsonschema.DefaultSchemaValidatorFactory;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.resources.DefaultResourceLoaderFactory;
import io.serverlessworkflow.resources.ResourceLoaderFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowApplication implements AutoCloseable {

  private final TaskExecutorFactory taskFactory;
  private final ExpressionFactory exprFactory;
  private final ResourceLoaderFactory resourceLoaderFactory;
  private final SchemaValidatorFactory schemaValidatorFactory;
  private final Collection<WorkflowExecutionListener> listeners;
  private final Map<WorkflowId, WorkflowDefinition> definitions;

  public WorkflowApplication(
      TaskExecutorFactory taskFactory,
      ExpressionFactory exprFactory,
      ResourceLoaderFactory resourceLoaderFactory,
      SchemaValidatorFactory schemaValidatorFactory,
      Collection<WorkflowExecutionListener> listeners) {
    this.taskFactory = taskFactory;
    this.exprFactory = exprFactory;
    this.resourceLoaderFactory = resourceLoaderFactory;
    this.schemaValidatorFactory = schemaValidatorFactory;
    this.listeners = listeners;
    this.definitions = new ConcurrentHashMap<>();
  }

  public TaskExecutorFactory taskFactory() {
    return taskFactory;
  }

  public static Builder builder() {
    return new Builder();
  }

  public ExpressionFactory expressionFactory() {
    return exprFactory;
  }

  public SchemaValidatorFactory validatorFactory() {
    return schemaValidatorFactory;
  }

  public ResourceLoaderFactory resourceLoaderFactory() {
    return resourceLoaderFactory;
  }

  public Collection<WorkflowExecutionListener> listeners() {
    return listeners;
  }

  public static class Builder {
    private TaskExecutorFactory taskFactory = DefaultTaskExecutorFactory.get();
    private ExpressionFactory exprFactory = JQExpressionFactory.get();
    private Collection<WorkflowExecutionListener> listeners;
    private ResourceLoaderFactory resourceLoaderFactory = DefaultResourceLoaderFactory.get();
    private SchemaValidatorFactory schemaValidatorFactory = DefaultSchemaValidatorFactory.get();

    private Builder() {}

    public Builder withListener(WorkflowExecutionListener listener) {
      if (listeners == null) {
        listeners = new HashSet<>();
      }
      listeners.add(listener);
      return this;
    }

    public Builder withTaskExecutorFactory(TaskExecutorFactory factory) {
      this.taskFactory = factory;
      return this;
    }

    public Builder withExpressionFactory(ExpressionFactory factory) {
      this.exprFactory = factory;
      return this;
    }

    public Builder withResourceLoaderFactory(ResourceLoaderFactory resourceLoader) {
      this.resourceLoaderFactory = resourceLoader;
      return this;
    }

    public Builder withSchemaValidatorFactory(SchemaValidatorFactory factory) {
      this.schemaValidatorFactory = factory;
      return this;
    }

    public WorkflowApplication build() {
      return new WorkflowApplication(
          taskFactory,
          exprFactory,
          resourceLoaderFactory,
          schemaValidatorFactory,
          listeners == null
              ? Collections.emptySet()
              : Collections.unmodifiableCollection(listeners));
    }
  }

  private static record WorkflowId(String namespace, String name, String version) {
    static WorkflowId of(Document document) {
      return new WorkflowId(document.getNamespace(), document.getName(), document.getVersion());
    }
  }

  public WorkflowDefinition workflowDefinition(Workflow workflow) {
    return definitions.computeIfAbsent(
        WorkflowId.of(workflow.getDocument()), k -> WorkflowDefinition.of(this, workflow));
  }

  @Override
  public void close() throws Exception {
    for (WorkflowDefinition definition : definitions.values()) {
      definition.close();
    }
    definitions.clear();
  }
}
