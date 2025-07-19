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
package io.serverlessworkflow.impl.expressions;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import java.time.OffsetDateTime;
import java.util.Map;

class JavaModelFactory implements WorkflowModelFactory {

  @Override
  public WorkflowModel combine(Map<String, WorkflowModel> workflowVariables) {
    return new JavaModel(workflowVariables);
  }

  @Override
  public WorkflowModelCollection createCollection() {
    return new JavaModelCollection();
  }

  @Override
  public WorkflowModel from(boolean value) {
    return value ? JavaModel.TrueModel : JavaModel.FalseModel;
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
    return new JavaModel(map);
  }

  @Override
  public WorkflowModel fromNull() {
    return JavaModel.NullModel;
  }

  @Override
  public WorkflowModel fromAny(Object obj) {
    return new JavaModel(obj);
  }
}
