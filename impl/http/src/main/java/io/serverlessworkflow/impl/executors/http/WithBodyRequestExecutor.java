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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.auth.AuthProvider;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

class WithBodyRequestExecutor extends AbstractRequestExecutor {
  private final WorkflowFilter bodyFilter;

  public WithBodyRequestExecutor(
      String method,
      boolean redirect,
      Optional<AuthProvider> auth,
      WorkflowApplication application,
      Object body) {
    super(method, redirect, auth);
    bodyFilter = WorkflowUtils.buildWorkflowFilter(application, body);
  }

  @Override
  protected Response invokeRequest(
      Builder request,
      HttpModelConverter converter,
      WorkflowContext workflow,
      TaskContext task,
      WorkflowModel model) {
    return request.method(method, converter.toEntity(bodyFilter.apply(workflow, task, model)));
  }
}
