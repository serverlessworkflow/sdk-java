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
package io.serverlessworkflow.impl.expressions.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.expressions.agentic.langchain4j.AgenticScopeRegistryAssessor;
import io.serverlessworkflow.impl.expressions.func.JavaModel;
import java.time.OffsetDateTime;
import java.util.Map;

class AgenticModelFactory implements WorkflowModelFactory {

  private static final String DEFAULT_AGENTIC_SCOPE_STATE_KEY = "input";
  private final AgenticScopeRegistryAssessor scopeRegistryAssessor =
      new AgenticScopeRegistryAssessor();

  private AgenticModel asAgenticModel(Object value) {
    // TODO: fetch memoryId from the object based on known premises
    final AgenticScope agenticScope = this.scopeRegistryAssessor.getAgenticScope();
    if (value != null) {
      agenticScope.writeState(DEFAULT_AGENTIC_SCOPE_STATE_KEY, value);
    }
    return new AgenticModel(agenticScope);
  }

  /**
   * Applies any change to the model after running as task. We will always set it to a @AgenticScope
   * object since @AgentExecutor is always adding the output to the agenticScope. We just have to
   * make sure that agenticScope is always passed to the next input task.
   *
   * @param prev the global AgenticScope object getting updated by the workflow context
   * @param obj the same AgenticScope object updated by the AgentExecutor
   * @return the workflow context model holding the agenticScope object.
   */
  @Override
  public WorkflowModel fromAny(WorkflowModel prev, Object obj) {
    // We ignore `obj` since it's already included in `prev` within the agenticScope instance
    return prev;
  }

  @Override
  public WorkflowModel combine(Map<String, WorkflowModel> workflowVariables) {
    // TODO: create a new agenticScope object in the AgenticScopeRegistryAssessor per branch
    // TODO: Since we share the same agenticScope object, both branches are updating the same
    // instance, so for now we return the first key.
    return workflowVariables.values().iterator().next();
  }

  @Override
  public WorkflowModelCollection createCollection() {
    throw new UnsupportedOperationException();
  }

  // TODO: all these methods can use agenticScope as long as we have access to the `outputName`

  @Override
  public WorkflowModel from(boolean value) {
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(Number value) {
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(String value) {
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(CloudEvent ce) {
    // TODO: serialize the CE into the AgenticScope
    return new JavaModel(ce);
  }

  @Override
  public WorkflowModel from(CloudEventData ce) {
    // TODO: serialize the CE data into the AgenticScope
    return new JavaModel(ce);
  }

  @Override
  public WorkflowModel from(OffsetDateTime value) {
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(Map<String, Object> map) {
    final AgenticScope agenticScope = this.scopeRegistryAssessor.getAgenticScope();
    agenticScope.writeStates(map);
    return new AgenticModel(agenticScope);
  }

  @Override
  public WorkflowModel fromNull() {
    return asAgenticModel(null);
  }

  @Override
  public WorkflowModel fromOther(Object value) {
    if (value instanceof AgenticScope) {
      return new AgenticModel((AgenticScope) value);
    }
    return asAgenticModel(value);
  }
}
