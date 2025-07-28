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
package io.serverlessworkflow.impl.executors.http;

import io.serverlessworkflow.api.types.DigestAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import jakarta.ws.rs.client.Invocation.Builder;

public class DigestAuthProvider implements AuthProvider {

  public DigestAuthProvider(
      WorkflowApplication app, Workflow workflow, DigestAuthenticationPolicy authPolicy) {
    throw new UnsupportedOperationException("Digest auth not supported yet");
  }

  @Override
  public Builder build(
      Builder builder, WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    // TODO Auto-generated method stub
    return builder;
  }
}
