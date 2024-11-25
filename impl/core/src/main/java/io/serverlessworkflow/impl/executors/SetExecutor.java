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
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.json.MergeUtils;

public class SetExecutor extends AbstractTaskExecutor<SetTask> {

  private JsonNode toBeSet;

  protected SetExecutor(SetTask task, WorkflowDefinition definition) {
    super(task, definition);
    this.toBeSet = JsonUtils.fromValue(task.getSet().getAdditionalProperties());
  }

  @Override
  protected void internalExecute(WorkflowContext workflow, TaskContext<SetTask> taskContext) {
    taskContext.rawOutput(MergeUtils.merge(toBeSet, taskContext.input()));
  }
}
