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

import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SwitchExecutor extends AbstractTaskExecutor<SwitchTask> {

  private final Map<WorkflowFilter, TransitionInfo> workflowFilters;
  private final TransitionInfo defaultTask;

  public static class SwitchExecutorBuilder extends AbstractTaskExecutorBuilder<SwitchTask> {
    private final Map<SwitchCase, WorkflowFilter> workflowFilters = new HashMap<>();
    private Map<WorkflowFilter, TransitionInfoBuilder> switchFilters = new HashMap<>();
    private FlowDirective defaultDirective;
    private TransitionInfoBuilder defaultTask;

    public SwitchExecutorBuilder(
        WorkflowPosition position,
        SwitchTask task,
        Workflow workflow,
        WorkflowApplication application,
        ResourceLoader resourceLoader) {
      super(position, task, workflow, application, resourceLoader);
      for (SwitchItem item : task.getSwitch()) {
        SwitchCase switchCase = item.getSwitchCase();
        if (switchCase.getWhen() != null) {
          workflowFilters.put(
              switchCase,
              WorkflowUtils.buildWorkflowFilter(
                  application.expressionFactory(), switchCase.getWhen()));
        } else {
          defaultDirective = switchCase.getThen();
        }
      }
    }

    @Override
    public void connect(Map<String, TaskExecutorBuilder<?>> connections) {
      this.switchFilters =
          this.workflowFilters.entrySet().stream()
              .collect(
                  Collectors.toMap(Entry::getValue, e -> next(e.getKey().getThen(), connections)));
      this.defaultTask = next(defaultDirective, connections);
    }

    @Override
    protected TaskExecutor<SwitchTask> buildInstance() {
      return new SwitchExecutor(this);
    }
  }

  private SwitchExecutor(SwitchExecutorBuilder builder) {
    super(builder);
    this.defaultTask = TransitionInfo.build(builder.defaultTask);
    this.workflowFilters =
        builder.switchFilters.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> TransitionInfo.build(e.getValue())));
  }

  @Override
  protected CompletableFuture<TaskContext> execute(
      WorkflowContext workflow, TaskContext taskContext) {
    CompletableFuture<TaskContext> future = CompletableFuture.completedFuture(taskContext);
    for (Entry<WorkflowFilter, TransitionInfo> entry : workflowFilters.entrySet()) {
      if (entry.getKey().apply(workflow, taskContext, taskContext.input()).asBoolean()) {
        return future.thenApply(t -> t.transition(entry.getValue()));
      }
    }
    return future.thenApply(t -> t.transition(defaultTask));
  }
}
