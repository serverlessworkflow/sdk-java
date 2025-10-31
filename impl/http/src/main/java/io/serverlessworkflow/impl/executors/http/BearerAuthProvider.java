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

import io.serverlessworkflow.api.types.BearerAuthenticationPolicy;
import io.serverlessworkflow.api.types.BearerAuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;

class BearerAuthProvider extends AbstractAuthProvider {

  private static final String BEARER_TOKEN = "Bearer %s";

  private WorkflowValueResolver<String> tokenFilter;

  public BearerAuthProvider(
      WorkflowApplication app,
      Workflow workflow,
      BearerAuthenticationPolicy basicAuthenticationPolicy) {
    BearerAuthenticationPolicyConfiguration config = basicAuthenticationPolicy.getBearer();
    if (config.getBearerAuthenticationProperties() != null) {
      String token = config.getBearerAuthenticationProperties().getToken();
      tokenFilter = WorkflowUtils.buildStringFilter(app, token);
    } else if (config.getBearerAuthenticationPolicySecret() != null) {
      String secretName = checkSecret(workflow, config.getBearerAuthenticationPolicySecret());
      tokenFilter = (w, t, m) -> find(w, secretName, "bearer");
    }
  }

  @Override
  protected String authHeader(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    return String.format(BEARER_TOKEN, tokenFilter.apply(workflow, task, model));
  }
}
