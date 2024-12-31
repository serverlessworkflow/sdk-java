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
package io.serverlessworkflow.impl.executors;

import static io.serverlessworkflow.impl.WorkflowUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractTaskExecutor<T extends TaskBase> implements TaskExecutor<T> {

  protected final T task;
  protected final String taskName;
  protected final WorkflowPosition position;
  private final Optional<WorkflowFilter> inputProcessor;
  private final Optional<WorkflowFilter> outputProcessor;
  private final Optional<WorkflowFilter> contextProcessor;
  private final Optional<SchemaValidator> inputSchemaValidator;
  private final Optional<SchemaValidator> outputSchemaValidator;
  private final Optional<SchemaValidator> contextSchemaValidator;

  public abstract static class AbstractTaskExecutorBuilder<T extends TaskBase>
      implements TaskExecutorBuilder<T> {
    private Optional<WorkflowFilter> inputProcessor = Optional.empty();
    private Optional<WorkflowFilter> outputProcessor = Optional.empty();
    private Optional<WorkflowFilter> contextProcessor = Optional.empty();
    private Optional<SchemaValidator> inputSchemaValidator = Optional.empty();
    private Optional<SchemaValidator> outputSchemaValidator = Optional.empty();
    private Optional<SchemaValidator> contextSchemaValidator = Optional.empty();
    protected final WorkflowPosition position;
    protected final T task;
    protected final String taskName;
    protected final WorkflowApplication application;
    protected final Workflow workflow;
    protected final ResourceLoader resourceLoader;

    private TaskExecutor<T> instance;

    protected AbstractTaskExecutorBuilder(
        WorkflowPosition position,
        T task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      this.workflow = workflow;
      this.taskName = position.last().toString();
      this.position = position;
      this.task = task;
      this.application = application;
      this.resourceLoader = resourceLoader;
      if (task.getInput() != null) {
        Input input = task.getInput();
        this.inputProcessor = buildWorkflowFilter(application.expressionFactory(), input.getFrom());
        this.inputSchemaValidator =
            getSchemaValidator(application.validatorFactory(), resourceLoader, input.getSchema());
      }
      if (task.getOutput() != null) {
        Output output = task.getOutput();
        this.outputProcessor = buildWorkflowFilter(application.expressionFactory(), output.getAs());
        this.outputSchemaValidator =
            getSchemaValidator(application.validatorFactory(), resourceLoader, output.getSchema());
      }
      if (task.getExport() != null) {
        Export export = task.getExport();
        if (export.getAs() != null) {
          this.contextProcessor =
              buildWorkflowFilter(application.expressionFactory(), export.getAs());
        }
        this.contextSchemaValidator =
            getSchemaValidator(application.validatorFactory(), resourceLoader, export.getSchema());
      }
    }

    protected final TransitionInfoBuilder next(
        FlowDirective flowDirective, Map<String, TaskExecutorBuilder<?>> connections) {
      if (flowDirective == null) {
        return TransitionInfoBuilder.of(next(connections));
      }
      if (flowDirective.getFlowDirectiveEnum() != null) {
        switch (flowDirective.getFlowDirectiveEnum()) {
          case CONTINUE:
            return TransitionInfoBuilder.of(next(connections));
          case END:
            return TransitionInfoBuilder.end();
          case EXIT:
            return TransitionInfoBuilder.exit();
        }
      }
      return TransitionInfoBuilder.of(connections.get(flowDirective.getString()));
    }

    private TaskExecutorBuilder<?> next(Map<String, TaskExecutorBuilder<?>> connections) {
      Iterator<TaskExecutorBuilder<?>> iter = connections.values().iterator();
      TaskExecutorBuilder<?> next = null;
      while (iter.hasNext()) {
        TaskExecutorBuilder<?> item = iter.next();
        if (item == this) {
          next = iter.hasNext() ? iter.next() : null;
          break;
        }
      }
      return next;
    }

    public TaskExecutor<T> build() {
      if (instance == null) {
        instance = buildInstance();
      }
      return instance;
    }

    protected abstract TaskExecutor<T> buildInstance();
  }

  protected AbstractTaskExecutor(AbstractTaskExecutorBuilder<T> builder) {
    this.task = builder.task;
    this.taskName = builder.taskName;
    this.position = builder.position;
    this.inputProcessor = builder.inputProcessor;
    this.outputProcessor = builder.outputProcessor;
    this.contextProcessor = builder.contextProcessor;
    this.inputSchemaValidator = builder.inputSchemaValidator;
    this.outputSchemaValidator = builder.outputSchemaValidator;
    this.contextSchemaValidator = builder.contextSchemaValidator;
  }

  protected final CompletableFuture<TaskContext> executeNext(
      CompletableFuture<TaskContext> future, WorkflowContext workflow) {
    return future.thenCompose(
        t -> {
          TransitionInfo transition = t.transition();
          if (transition.isEndNode()) {
            workflow.instance().status(WorkflowStatus.COMPLETED);
          } else if (transition.next() != null) {
            return transition.next().apply(workflow, t.parent(), t.output());
          }
          return CompletableFuture.completedFuture(t);
        });
  }

  @Override
  public CompletableFuture<TaskContext> apply(
      WorkflowContext workflowContext, Optional<TaskContext> parentContext, JsonNode input) {
    TaskContext taskContext = new TaskContext(input, position, parentContext, taskName, task);
    CompletableFuture<TaskContext> completable = CompletableFuture.completedFuture(taskContext);
    if (!TaskExecutorHelper.isActive(workflowContext)) {
      return completable;
    }
    return executeNext(
        completable
            .thenApply(
                t -> {
                  workflowContext
                      .definition()
                      .listeners()
                      .forEach(l -> l.onTaskStarted(position, task));
                  inputSchemaValidator.ifPresent(s -> s.validate(t.rawInput()));
                  inputProcessor.ifPresent(
                      p -> taskContext.input(p.apply(workflowContext, t, t.rawInput())));
                  return t;
                })
            .thenCompose(t -> execute(workflowContext, t))
            .thenApply(
                t -> {
                  outputProcessor.ifPresent(
                      p -> t.output(p.apply(workflowContext, t, t.rawOutput())));
                  outputSchemaValidator.ifPresent(s -> s.validate(t.output()));
                  contextProcessor.ifPresent(
                      p ->
                          workflowContext.context(
                              p.apply(workflowContext, t, workflowContext.context())));
                  contextSchemaValidator.ifPresent(s -> s.validate(workflowContext.context()));
                  t.completedAt(Instant.now());
                  workflowContext
                      .definition()
                      .listeners()
                      .forEach(l -> l.onTaskEnded(position, task));
                  return t;
                }),
        workflowContext);
  }

  protected abstract CompletableFuture<TaskContext> execute(
      WorkflowContext workflow, TaskContext taskContext);
}
