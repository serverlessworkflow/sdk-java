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

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Iterator;
import java.util.Optional;

public class ForExecutor extends AbstractTaskExecutor<ForTask> {

  private final WorkflowFilter collectionExpr;
  private final Optional<WorkflowFilter> whileExpr;

  protected ForExecutor(ForTask task, WorkflowDefinition definition) {
    super(task, definition);
    ForTaskConfiguration forConfig = task.getFor();
    this.collectionExpr =
        WorkflowUtils.buildWorkflowFilter(definition.expressionFactory(), forConfig.getIn());
    this.whileExpr = WorkflowUtils.optionalFilter(definition.expressionFactory(), task.getWhile());
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<ForTask> taskContext) {
    Iterator<JsonNode> iter =
        collectionExpr.apply(workflow, taskContext, taskContext.input()).iterator();
    int i = 0;
    while (iter.hasNext()
        && whileExpr
            .<JsonNode>map(w -> w.apply(workflow, taskContext, taskContext.rawOutput()))
            .map(n -> n.asBoolean(true))
            .orElse(true)) {
      JsonNode item = iter.next();
      taskContext.variables().put(task.getFor().getEach(), item);
      taskContext.variables().put(task.getFor().getAt(), i++);
      taskContext.rawOutput(WorkflowUtils.processTaskList(task.getDo(), workflow, taskContext));
    }
  }
}
