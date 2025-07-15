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

import com.github.f4b6a3.ulid.UlidCreator;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventPublisher;
import io.serverlessworkflow.impl.events.InMemoryEvents;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.JQExpressionFactory;
import io.serverlessworkflow.impl.expressions.RuntimeDescriptor;
import io.serverlessworkflow.impl.jsonschema.DefaultSchemaValidatorFactory;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.impl.resources.DefaultResourceLoaderFactory;
import io.serverlessworkflow.impl.resources.ResourceLoaderFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowApplication implements AutoCloseable {

  private final TaskExecutorFactory taskFactory;
  private final ExpressionFactory exprFactory;
  private final ResourceLoaderFactory resourceLoaderFactory;
  private final SchemaValidatorFactory schemaValidatorFactory;
  private final WorkflowIdFactory idFactory;
  private final Collection<WorkflowExecutionListener> listeners;
  private final Map<WorkflowId, WorkflowDefinition> definitions;
  private final WorkflowPositionFactory positionFactory;
  private final ExecutorServiceFactory executorFactory;
  private final RuntimeDescriptorFactory runtimeDescriptorFactory;
  private final EventConsumer<?, ?> eventConsumer;
  private final EventPublisher eventPublisher;

  private WorkflowApplication(Builder builder) {
    this.taskFactory = builder.taskFactory;
    this.exprFactory = builder.exprFactory;
    this.resourceLoaderFactory = builder.resourceLoaderFactory;
    this.schemaValidatorFactory = builder.schemaValidatorFactory;
    this.positionFactory = builder.positionFactory;
    this.idFactory = builder.idFactory;
    this.runtimeDescriptorFactory = builder.descriptorFactory;
    this.executorFactory = builder.executorFactory;
    this.listeners = builder.listeners != null ? builder.listeners : Collections.emptySet();
    this.definitions = new ConcurrentHashMap<>();
    this.eventConsumer = builder.eventConsumer;
    this.eventPublisher = builder.eventPublisher;
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

  public EventPublisher eventPublisher() {
    return eventPublisher;
  }

  public WorkflowIdFactory idFactory() {
    return idFactory;
  }

  public static class Builder {
    private TaskExecutorFactory taskFactory = DefaultTaskExecutorFactory.get();
    private ExpressionFactory exprFactory = JQExpressionFactory.get();
    private Collection<WorkflowExecutionListener> listeners;
    private ResourceLoaderFactory resourceLoaderFactory = DefaultResourceLoaderFactory.get();
    private SchemaValidatorFactory schemaValidatorFactory = DefaultSchemaValidatorFactory.get();
    private WorkflowPositionFactory positionFactory = () -> new QueueWorkflowPosition();
    private WorkflowIdFactory idFactory = () -> UlidCreator.getMonotonicUlid().toString();
    private ExecutorServiceFactory executorFactory = () -> Executors.newCachedThreadPool();
    private EventConsumer<?, ?> eventConsumer = InMemoryEvents.get();
    private EventPublisher eventPublisher = InMemoryEvents.get();
    private RuntimeDescriptorFactory descriptorFactory =
        () -> new RuntimeDescriptor("reference impl", "1.0.0_alpha", Collections.emptyMap());

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

    public Builder withExecutorFactory(ExecutorServiceFactory executorFactory) {
      this.executorFactory = executorFactory;
      return this;
    }

    public Builder withPositionFactory(WorkflowPositionFactory positionFactory) {
      this.positionFactory = positionFactory;
      return this;
    }

    public Builder withSchemaValidatorFactory(SchemaValidatorFactory factory) {
      this.schemaValidatorFactory = factory;
      return this;
    }

    public Builder withIdFactory(WorkflowIdFactory factory) {
      this.idFactory = factory;
      return this;
    }

    public Builder withDescriptorFactory(RuntimeDescriptorFactory factory) {
      this.descriptorFactory = factory;
      return this;
    }

    public Builder withEventConsumer(EventConsumer<?, ?> eventConsumer) {
      this.eventConsumer = eventConsumer;
      return this;
    }

    public Builder withEventPublisher(EventPublisher eventPublisher) {
      this.eventPublisher = eventPublisher;
      return this;
    }

    public WorkflowApplication build() {
      return new WorkflowApplication(this);
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
  public void close() {
    for (WorkflowDefinition definition : definitions.values()) {
      definition.close();
    }
    definitions.clear();
  }

  public WorkflowPositionFactory positionFactory() {
    return positionFactory;
  }

  public WorkflowModelFactory modelFactory() {
    return exprFactory.modelFactory();
  }

  public RuntimeDescriptorFactory runtimeDescriptorFactory() {
    return runtimeDescriptorFactory;
  }

  @SuppressWarnings("rawtypes")
  public EventConsumer eventConsumer() {
    return eventConsumer;
  }

  public ExecutorService executorService() {
    return executorFactory.get();
  }
}
