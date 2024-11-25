package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SwitchExecutor extends AbstractTaskExecutor<SwitchTask> {

  private Map<SwitchCase, WorkflowFilter> workflowFilters = new ConcurrentHashMap<>();
  private FlowDirective defaultDirective;

  protected SwitchExecutor(SwitchTask task, WorkflowDefinition definition) {
    super(task, definition);
    for (SwitchItem item : task.getSwitch()) {
      SwitchCase switchCase = item.getSwitchCase();
      if (switchCase.getWhen() != null) {
        workflowFilters.put(
            switchCase,
            WorkflowUtils.buildWorkflowFilter(
                definition.expressionFactory(), switchCase.getWhen()));
      } else {
        defaultDirective = switchCase.getThen();
      }
    }
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<SwitchTask> taskContext) {
    for (Entry<SwitchCase, WorkflowFilter> entry : workflowFilters.entrySet()) {
      if (entry
          .getValue()
          .apply(workflow, Optional.of(taskContext), taskContext.input())
          .asBoolean()) {
        taskContext.flowDirective(entry.getKey().getThen());
        return;
      }
    }
    taskContext.flowDirective(defaultDirective);
  }
}
