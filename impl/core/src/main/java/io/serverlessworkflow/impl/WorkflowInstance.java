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

import static io.serverlessworkflow.impl.json.JsonUtils.toJavaValue;

import com.fasterxml.jackson.databind.JsonNode;

public class WorkflowInstance {
  private WorkflowState state;
  private WorkflowContext context;
  private TaskContext<?> taskContext;

  WorkflowInstance(WorkflowDefinition definition, JsonNode input) {
    definition.inputSchemaValidator().ifPresent(v -> v.validate(input));
    context = new WorkflowContext(definition, input);
    taskContext = new TaskContext<>(input, definition.positionFactory().buildPosition());
    definition
        .inputFilter()
        .ifPresent(f -> taskContext.input(f.apply(context, taskContext, input)));
    state = WorkflowState.STARTED;
    taskContext.rawOutput(
        WorkflowUtils.processTaskList(definition.workflow().getDo(), context, taskContext));
    definition
        .outputFilter()
        .ifPresent(f -> taskContext.output(f.apply(context, taskContext, taskContext.rawOutput())));
    definition.outputSchemaValidator().ifPresent(v -> v.validate(taskContext.output()));
  }

  public WorkflowState state() {
    return state;
  }

  public Object output() {
    return toJavaValue(taskContext.output());
  }

  public Object outputAsJsonNode() {
    return taskContext.output();
  }
}
