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

import static io.serverlessworkflow.impl.WorkflowUtils.safeClose;

import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.additional.WorkflowAdditionalObject;
import io.serverlessworkflow.impl.config.ConfigManager;
import io.serverlessworkflow.impl.config.ConfigSecretManager;
import io.serverlessworkflow.impl.config.SecretManager;
import io.serverlessworkflow.impl.config.SystemPropertyConfigManager;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventPublisher;
import io.serverlessworkflow.impl.events.InMemoryEvents;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.RuntimeDescriptor;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import io.serverlessworkflow.impl.resources.DefaultResourceLoaderFactory;
import io.serverlessworkflow.impl.resources.ExternalResourceHandler;
import io.serverlessworkflow.impl.resources.ResourceLoaderFactory;
import io.serverlessworkflow.impl.scheduler.DefaultWorkflowScheduler;
import io.serverlessworkflow.impl.schema.SchemaValidator;
import io.serverlessworkflow.impl.schema.SchemaValidatorFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class WorkflowApplication implements AutoCloseable {

  private final TaskExecutorFactory taskFactory;
  private final ExpressionFactory exprFactory;
  private final ResourceLoaderFactory resourceLoaderFactory;
  private final SchemaValidatorFactory schemaValidatorFactory;
  private final WorkflowInstanceIdFactory idFactory;
  private final Collection<WorkflowExecutionListener> listeners;
  private final Map<WorkflowDefinitionId, WorkflowDefinition> definitions;
  private final WorkflowPositionFactory positionFactory;
  private final ExecutorServiceFactory executorFactory;
  private final RuntimeDescriptorFactory runtimeDescriptorFactory;
  private final EventConsumer<?, ?> eventConsumer;
  private final Collection<EventPublisher> eventPublishers;
  private final boolean lifeCycleCEPublishingEnabled;
  private final WorkflowModelFactory modelFactory;
  private final WorkflowScheduler scheduler;
  private final Map<String, WorkflowAdditionalObject<?>> additionalObjects;
  private final ConfigManager configManager;
  private final SecretManager secretManager;

  private WorkflowApplication(Builder builder) {
    this.taskFactory = builder.taskFactory;
    this.exprFactory = new CompositeExpressionFactory(builder.exprFactories);
    this.resourceLoaderFactory = builder.resourceLoaderFactory;
    this.schemaValidatorFactory = builder.schemaValidatorFactory;
    this.positionFactory = builder.positionFactory;
    this.idFactory = builder.idFactory;
    this.runtimeDescriptorFactory = builder.descriptorFactory;
    this.executorFactory = builder.executorFactory;
    this.listeners = builder.listeners != null ? builder.listeners : Collections.emptySet();
    this.definitions = new ConcurrentHashMap<>();
    this.eventConsumer = builder.eventConsumer;
    this.eventPublishers = builder.eventPublishers;
    this.lifeCycleCEPublishingEnabled = builder.lifeCycleCEPublishingEnabled;
    this.modelFactory = builder.modelFactory;
    this.scheduler = builder.scheduler;
    this.additionalObjects = builder.additionalObjects;
    this.configManager = builder.configManager;
    this.secretManager = builder.secretManager;
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

  public Collection<EventPublisher> eventPublishers() {
    return eventPublishers;
  }

  public WorkflowInstanceIdFactory idFactory() {
    return idFactory;
  }

  public static class Builder {

    private static final class EmptySchemaValidatorHolder {
      private static final SchemaValidatorFactory instance =
          new SchemaValidatorFactory() {
            private final SchemaValidator NoValidation =
                new SchemaValidator() {
                  @Override
                  public void validate(WorkflowModel node) {}
                };

            @Override
            public SchemaValidator getValidator(ExternalResourceHandler resource) {
              return NoValidation;
            }

            @Override
            public SchemaValidator getValidator(SchemaInline inline) {
              return NoValidation;
            }
          };
    }

    private TaskExecutorFactory taskFactory;
    private Collection<ExpressionFactory> exprFactories = new HashSet<>();
    private Collection<WorkflowExecutionListener> listeners =
        ServiceLoader.load(WorkflowExecutionListener.class).stream()
            .map(Provider::get)
            .collect(Collectors.toList());
    private ResourceLoaderFactory resourceLoaderFactory = DefaultResourceLoaderFactory.get();
    private SchemaValidatorFactory schemaValidatorFactory;
    private WorkflowPositionFactory positionFactory = () -> new QueueWorkflowPosition();
    private WorkflowInstanceIdFactory idFactory;
    private WorkflowScheduler scheduler;
    private ExecutorServiceFactory executorFactory = new DefaultExecutorServiceFactory();
    private EventConsumer<?, ?> eventConsumer;
    private Collection<EventPublisher> eventPublishers = new ArrayList<>();
    private RuntimeDescriptorFactory descriptorFactory =
        () -> new RuntimeDescriptor("reference impl", "1.0.0_alpha", Collections.emptyMap());
    private boolean lifeCycleCEPublishingEnabled = true;
    private WorkflowModelFactory modelFactory;
    private Map<String, WorkflowAdditionalObject<?>> additionalObjects;
    private SecretManager secretManager;
    private ConfigManager configManager;

    private Builder() {}

    public Builder withListener(WorkflowExecutionListener listener) {
      listeners.add(listener);
      return this;
    }

    public Builder withTaskExecutorFactory(TaskExecutorFactory factory) {
      this.taskFactory = factory;
      return this;
    }

    public Builder withExpressionFactory(ExpressionFactory factory) {
      this.exprFactories.add(factory);
      return this;
    }

    public Builder withScheduler(WorkflowScheduler scheduler) {
      this.scheduler = scheduler;
      return this;
    }

    public Builder withResourceLoaderFactory(ResourceLoaderFactory resourceLoader) {
      this.resourceLoaderFactory = resourceLoader;
      return this;
    }

    public Builder disableLifeCycleCEPublishing() {
      this.lifeCycleCEPublishingEnabled = false;
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

    public Builder withIdFactory(WorkflowInstanceIdFactory factory) {
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
      this.eventPublishers.add(eventPublisher);
      return this;
    }

    public Builder withSecretManager(SecretManager secretManager) {
      this.secretManager = secretManager;
      return this;
    }

    public Builder withConfigManager(ConfigManager configManager) {
      this.configManager = configManager;
      return this;
    }

    public <T> Builder withAdditionalObject(
        String name, WorkflowAdditionalObject<T> additionalObject) {
      if (additionalObjects == null) {
        additionalObjects = new HashMap<>();
      }
      additionalObjects.put(name, additionalObject);
      return this;
    }

    public Builder withModelFactory(WorkflowModelFactory modelFactory) {
      this.modelFactory = modelFactory;
      return this;
    }

    public WorkflowApplication build() {
      if (modelFactory == null) {
        modelFactory =
            ServiceLoader.load(WorkflowModelFactory.class)
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "WorkflowModelFactory instance has to be set in WorkflowApplication or present in the classpath"));
      }
      ServiceLoader.load(ExpressionFactory.class).forEach(exprFactories::add);
      if (schemaValidatorFactory == null) {
        schemaValidatorFactory =
            ServiceLoader.load(SchemaValidatorFactory.class)
                .findFirst()
                .orElseGet(() -> EmptySchemaValidatorHolder.instance);
      }
      if (taskFactory == null) {
        taskFactory =
            ServiceLoader.load(TaskExecutorFactory.class)
                .findFirst()
                .orElseGet(() -> DefaultTaskExecutorFactory.get());
      }
      ServiceLoader.load(EventPublisher.class).forEach(e -> eventPublishers.add(e));
      if (eventConsumer == null) {
        eventConsumer =
            ServiceLoader.load(EventConsumer.class)
                .findFirst()
                .orElseGet(
                    () -> {
                      InMemoryEvents inMemory = new InMemoryEvents(executorFactory);
                      if (eventPublishers.isEmpty()) {
                        eventPublishers.add(inMemory);
                      }
                      return inMemory;
                    });
      }
      if (idFactory == null) {
        idFactory = new MonotonicUlidWorkflowInstanceIdFactory();
      }
      if (scheduler == null) {
        scheduler = new DefaultWorkflowScheduler();
      }
      if (additionalObjects == null) {
        additionalObjects = Collections.emptyMap();
      }
      if (configManager == null) {
        configManager =
            ServiceLoader.load(ConfigManager.class)
                .findFirst()
                .orElseGet(() -> new SystemPropertyConfigManager());
      }
      if (secretManager == null) {
        secretManager =
            ServiceLoader.load(SecretManager.class)
                .findFirst()
                .orElseGet(() -> new ConfigSecretManager(configManager));
      }
      return new WorkflowApplication(this);
    }
  }

  public Map<WorkflowDefinitionId, WorkflowDefinition> workflowDefinitions() {
    return Collections.unmodifiableMap(definitions);
  }

  public WorkflowDefinition workflowDefinition(Workflow workflow) {
    return definitions.computeIfAbsent(
        WorkflowDefinitionId.of(workflow), k -> WorkflowDefinition.of(this, workflow));
  }

  @Override
  public void close() {
    safeClose(executorFactory);
    for (EventPublisher eventPublisher : eventPublishers) {
      safeClose(eventPublisher);
    }
    safeClose(eventConsumer);

    for (WorkflowDefinition definition : definitions.values()) {
      safeClose(definition);
    }
    definitions.clear();

    for (WorkflowExecutionListener listener : listeners) {
      safeClose(listener);
    }
    listeners.clear();
  }

  public WorkflowPositionFactory positionFactory() {
    return positionFactory;
  }

  public WorkflowModelFactory modelFactory() {
    return modelFactory;
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

  public boolean isLifeCycleCEPublishingEnabled() {
    return lifeCycleCEPublishingEnabled;
  }

  public WorkflowScheduler scheduler() {
    return scheduler;
  }

  public ConfigManager configManager() {
    return configManager;
  }

  public SecretManager secretManager() {
    return secretManager;
  }

  public <T> Optional<T> additionalObject(
      String name, WorkflowContext workflowContext, TaskContext taskContext) {
    return Optional.ofNullable(additionalObjects.get(name))
        .map(v -> (T) v.apply(workflowContext, taskContext));
  }
}
