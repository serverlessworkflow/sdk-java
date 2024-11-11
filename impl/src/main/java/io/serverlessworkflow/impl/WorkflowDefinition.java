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

import static io.serverlessworkflow.impl.json.JsonUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.executors.DefaultTaskExecutorFactory;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutorFactory;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowDefinition {

  private WorkflowDefinition(
      Workflow workflow,
      TaskExecutorFactory taskFactory,
      Collection<WorkflowExecutionListener> listeners) {
    this.workflow = workflow;
    this.taskFactory = taskFactory;
    this.listeners = listeners;
  }

  private final Workflow workflow;
  private final Collection<WorkflowExecutionListener> listeners;
  private final TaskExecutorFactory taskFactory;
  private final Map<String, TaskExecutor<? extends TaskBase>> taskExecutors =
      new ConcurrentHashMap<>();

  public static class Builder {
    private final Workflow workflow;
    private TaskExecutorFactory taskFactory = DefaultTaskExecutorFactory.get();
    private Collection<WorkflowExecutionListener> listeners;

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

    public WorkflowDefinition build() {
      return new WorkflowDefinition(
          workflow,
          taskFactory,
          listeners == null
              ? Collections.emptySet()
              : Collections.unmodifiableCollection(listeners));
    }
  }

  public static Builder builder(Workflow workflow) {
    return new Builder(workflow);
  }

  public WorkflowInstance execute(Object input) {
    return new WorkflowInstance(taskFactory, JsonUtils.fromValue(input));
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

    private WorkflowInstance(TaskExecutorFactory factory, JsonNode input) {
      this.output = input;
      this.state = State.STARTED;
      this.context = WorkflowContext.builder(input).build();
      processDo(workflow.getDo());
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
                    k -> taskFactory.getTaskExecutor(task.getTask()))
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
