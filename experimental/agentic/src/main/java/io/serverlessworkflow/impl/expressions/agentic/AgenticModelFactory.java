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
import java.time.OffsetDateTime;
import java.util.Map;

class AgenticModelFactory implements WorkflowModelFactory {

  static final String DEFAULT_AGENTIC_SCOPE_STATE_KEY = "input";
  private final AgenticScopeRegistryAssessor scopeRegistryAssessor =
      new AgenticScopeRegistryAssessor();

  private void updateAgenticScope(Object value) {
    this.scopeRegistryAssessor.getAgenticScope().writeState(DEFAULT_AGENTIC_SCOPE_STATE_KEY, value);
  }

  private void updateAgenticScope(Map<String, Object> state) {
    this.scopeRegistryAssessor.getAgenticScope().writeStates(state);
  }

  private AgenticModel asAgenticModel(Object value) {
    return new AgenticModel(this.scopeRegistryAssessor.getAgenticScope(), value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public WorkflowModel fromAny(WorkflowModel prev, Object obj) {
    // TODO: we shouldn't update the state if the previous task was an agent call since under the
    // hood, the agent already updated it.
    if (prev instanceof AgenticModel agenticModel) {
      this.scopeRegistryAssessor.setAgenticScope(agenticModel.getAgenticScope());
    }

    if (obj instanceof Map) {
      this.updateAgenticScope((Map<String, Object>) obj);
    } else {
      this.updateAgenticScope(obj);
    }

    return asAgenticModel(obj);
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
    return new AgenticModelCollection(this.scopeRegistryAssessor.getAgenticScope());
  }

  @Override
  public WorkflowModel from(boolean value) {
    this.updateAgenticScope(value);
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(Number value) {
    this.updateAgenticScope(value);
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(String value) {
    this.updateAgenticScope(value);
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(CloudEvent ce) {
    return asAgenticModel(ce);
  }

  @Override
  public WorkflowModel from(CloudEventData ce) {
    return asAgenticModel(ce);
  }

  @Override
  public WorkflowModel from(OffsetDateTime value) {
    this.updateAgenticScope(value);
    return asAgenticModel(value);
  }

  @Override
  public WorkflowModel from(Map<String, Object> map) {
    this.updateAgenticScope(map);
    return asAgenticModel(map);
  }

  @Override
  public WorkflowModel fromNull() {
    return asAgenticModel(null);
  }

  @Override
  public WorkflowModel fromOther(Object value) {
    if (value instanceof AgenticScope scope) {
      return new AgenticModel(scope, scope.state());
    }
    this.updateAgenticScope(value);
    return asAgenticModel(value);
  }
}
