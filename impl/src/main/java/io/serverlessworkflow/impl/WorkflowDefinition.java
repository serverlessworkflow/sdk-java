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

import static io.serverlessworkflow.impl.WorkflowUtils.*;
import static io.serverlessworkflow.impl.json.JsonUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.JQExpressionFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.jsonschema.DefaultSchemaValidatorFactory;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.resources.DefaultResourceLoaderFactory;
import io.serverlessworkflow.resources.ResourceLoaderFactory;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowDefinition {

  private WorkflowDefinition(
      Workflow workflow,
      Collection<WorkflowExecutionListener> listeners,
      WorkflowFactories factories) {
    this.workflow = workflow;
    this.listeners = listeners;
    this.factories = factories;
    if (workflow.getInput() != null) {
      Input input = workflow.getInput();
      this.inputSchemaValidator =
          getSchemaValidator(
              factories.getValidatorFactory(), schemaToNode(factories, input.getSchema()));
      this.inputFilter = buildWorkflowFilter(factories.getExpressionFactory(), input.getFrom());
    }
    if (workflow.getOutput() != null) {
      Output output = workflow.getOutput();
      this.outputSchemaValidator =
          getSchemaValidator(
              factories.getValidatorFactory(), schemaToNode(factories, output.getSchema()));
      this.outputFilter = buildWorkflowFilter(factories.getExpressionFactory(), output.getAs());
    }
  }

  private final Workflow workflow;
  private final Collection<WorkflowExecutionListener> listeners;
  private final WorkflowFactories factories;
  private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
  private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
  private Optional<WorkflowFilter> inputFilter = Optional.empty();
  private Optional<WorkflowFilter> outputFilter = Optional.empty();

  private final Map<String, TaskExecutor<? extends TaskBase>> taskExecutors =
      new ConcurrentHashMap<>();

  public static class Builder {
    private final Workflow workflow;
    private TaskExecutorFactory taskFactory = DefaultTaskExecutorFactory.get();
    private ExpressionFactory exprFactory = JQExpressionFactory.get();
    private Collection<WorkflowExecutionListener> listeners;
    private ResourceLoaderFactory resourceLoaderFactory = DefaultResourceLoaderFactory.get();
    private SchemaValidatorFactory schemaValidatorFactory = DefaultSchemaValidatorFactory.get();
    private Path path;

    private Builder(Workflow workflow) {
      this.workflow = workflow;
    }

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

    public Builder withPath(Path path) {
      this.path = path;
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

    public WorkflowDefinition build() {
      WorkflowDefinition def =
          new WorkflowDefinition(
              workflow,
              listeners == null
                  ? Collections.emptySet()
                  : Collections.unmodifiableCollection(listeners),
              new WorkflowFactories(
                  taskFactory,
                  resourceLoaderFactory.getResourceLoader(path),
                  exprFactory,
                  schemaValidatorFactory));
      return def;
    }
  }

  public static Builder builder(Workflow workflow) {
    return new Builder(workflow);
  }

  public WorkflowInstance execute(Object input) {
    return new WorkflowInstance(JsonUtils.fromValue(input));
  }

  enum State {
    STARTED,
    WAITING,
    FINISHED
  };

  public class WorkflowInstance {

    private JsonNode output;
    private State state;
    private WorkflowContext context;

    private WorkflowInstance(JsonNode input) {
      this.output = input;
      inputSchemaValidator.ifPresent(v -> v.validate(input));
      this.context = WorkflowContext.builder(input).build();
      inputFilter.ifPresent(f -> output = f.apply(context, Optional.empty(), output));
      this.state = State.STARTED;
      processDo(workflow.getDo());
      outputFilter.ifPresent(f -> output = f.apply(context, Optional.empty(), output));
      outputSchemaValidator.ifPresent(v -> v.validate(output));
    }

    private void processDo(List<TaskItem> tasks) {
      context.position().addProperty("do");
      int index = 0;
      for (TaskItem task : tasks) {
        context.position().addIndex(++index).addProperty(task.getName());
        listeners.forEach(l -> l.onTaskStarted(context.position(), task.getTask()));
        this.output =
            taskExecutors
                .computeIfAbsent(
                    context.position().jsonPointer(),
                    k -> factories.getTaskFactory().getTaskExecutor(task.getTask(), factories))
                .apply(context, output);
        listeners.forEach(l -> l.onTaskEnded(context.position(), task.getTask()));
        context.position().back().back();
      }
    }

    public State state() {
      return state;
    }

    public Object output() {
      return toJavaValue(output);
    }

    public Object outputAsJsonNode() {
      return output;
    }
  }
}
