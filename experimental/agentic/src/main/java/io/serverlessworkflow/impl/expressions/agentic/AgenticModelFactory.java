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

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.expressions.agentic.langchain4j.CognisphereRegistryAssessor;
import io.serverlessworkflow.impl.expressions.func.JavaModel;
import java.time.OffsetDateTime;
import java.util.Map;

class AgenticModelFactory implements WorkflowModelFactory {

  /**
   * Applies any change to the model after running as task. We will always set it to
   * a @DefaultCognisphere object since @AgentExecutor is always adding the output to the
   * cognisphere. We just have to make sure that cognisphere is always passed to the next input
   * task.
   *
   * @param prev the global Cognisphere object getting updated by the workflow context
   * @param obj the same Cognisphere object updated by the AgentExecutor
   * @return the workflow context model holding the cognisphere object.
   */
  @Override
  public WorkflowModel fromAny(WorkflowModel prev, Object obj) {
    // We ignore `obj` since it's already included in `prev` within the Cognisphere instance
    return prev;
  }

  @Override
  public WorkflowModel combine(Map<String, WorkflowModel> workflowVariables) {
    // TODO: create a new cognisphere object in the CognisphereRegistryAssessor per branch
    // TODO: Since we share the same cognisphere object, both branches are updating the same
    // instance, so for now we return the first key.
    return workflowVariables.values().iterator().next();
  }

  @Override
  public WorkflowModelCollection createCollection() {
    throw new UnsupportedOperationException();
  }

  // TODO: all these methods can use Cognisphere as long as we have access to the `outputName`

  @Override
  public WorkflowModel from(boolean value) {
    return new JavaModel(value);
  }

  @Override
  public WorkflowModel from(Number value) {
    return new JavaModel(value);
  }

  @Override
  public WorkflowModel from(String value) {
    return new JavaModel(value);
  }

  @Override
  public WorkflowModel from(CloudEvent ce) {
    return new JavaModel(ce);
  }

  @Override
  public WorkflowModel from(CloudEventData ce) {
    return new JavaModel(ce);
  }

  @Override
  public WorkflowModel from(OffsetDateTime value) {
    return new JavaModel(value);
  }

  @Override
  public WorkflowModel from(Map<String, Object> map) {
    final Cognisphere cognisphere = new CognisphereRegistryAssessor().getCognisphere();
    cognisphere.writeStates(map);
    return new AgenticModel(cognisphere);
  }

  @Override
  public WorkflowModel fromNull() {
    return new JavaModel(null);
  }

  @Override
  public WorkflowModel fromOther(Object value) {
    if (value instanceof Cognisphere) {
      return new AgenticModel((Cognisphere) value);
    }
    return new JavaModel(value);
  }
}
