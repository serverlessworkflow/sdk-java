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
import dev.langchain4j.agentic.cognisphere.CognisphereRegistry;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import java.time.OffsetDateTime;
import java.util.Map;

class AgenticModelFactory implements WorkflowModelFactory {
  private final Cognisphere cognisphere = CognisphereRegistry.createEphemeralCognisphere();

  private final AgenticModel TrueModel = new AgenticModel(Boolean.TRUE, cognisphere);
  private final AgenticModel FalseModel = new AgenticModel(Boolean.FALSE, cognisphere);
  private final AgenticModel NullModel = new AgenticModel(null, cognisphere);

  @Override
  public WorkflowModel combine(Map<String, WorkflowModel> workflowVariables) {
    return new AgenticModel(workflowVariables, cognisphere);
  }

  @Override
  public WorkflowModelCollection createCollection() {
    return new AgenticModelCollection(cognisphere);
  }

  @Override
  public WorkflowModel from(boolean value) {
    return value ? TrueModel : FalseModel;
  }

  @Override
  public WorkflowModel from(Number value) {
    return new AgenticModel(value, cognisphere);
  }

  @Override
  public WorkflowModel from(String value) {
    return new AgenticModel(value, cognisphere);
  }

  @Override
  public WorkflowModel from(CloudEvent ce) {
    return new AgenticModel(ce, cognisphere);
  }

  @Override
  public WorkflowModel from(CloudEventData ce) {
    return new AgenticModel(ce, cognisphere);
  }

  @Override
  public WorkflowModel from(OffsetDateTime value) {
    return new AgenticModel(value, cognisphere);
  }

  @Override
  public WorkflowModel from(Map<String, Object> map) {
    cognisphere.writeStates(map);
    return new AgenticModel(map, cognisphere);
  }

  @Override
  public WorkflowModel fromNull() {
    return NullModel;
  }

  @Override
  public WorkflowModel fromOther(Object value) {
    return new AgenticModel(value, cognisphere);
  }
}
