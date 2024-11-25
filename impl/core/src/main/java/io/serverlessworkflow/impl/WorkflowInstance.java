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
import java.util.Optional;

public class WorkflowInstance {
  private WorkflowState state;
  private WorkflowContext context;

  WorkflowInstance(WorkflowDefinition definition, JsonNode input) {
    definition.inputSchemaValidator().ifPresent(v -> v.validate(input));
    context = WorkflowContext.builder(definition, input).build();
    definition
        .inputFilter()
        .ifPresent(f -> context.current(f.apply(context, Optional.empty(), context.current())));
    state = WorkflowState.STARTED;
    WorkflowUtils.processTaskList(definition.workflow().getDo(), context);
    definition
        .outputFilter()
        .ifPresent(f -> context.current(f.apply(context, Optional.empty(), context.current())));
    definition.outputSchemaValidator().ifPresent(v -> v.validate(context.current()));
  }

  public WorkflowState state() {
    return state;
  }

  public Object output() {
    return toJavaValue(context.current());
  }

  public Object outputAsJsonNode() {
    return context.current();
  }
}
