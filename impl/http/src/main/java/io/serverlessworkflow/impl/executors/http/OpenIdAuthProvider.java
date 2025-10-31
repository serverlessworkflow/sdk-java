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

import io.serverlessworkflow.api.types.OpenIdConnectAuthenticationPolicy;
import io.serverlessworkflow.api.types.OpenIdConnectAuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.AuthRequestBuilder;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.OpenIdRequestBuilder;

public class OpenIdAuthProvider extends AbstractAuthProvider {

  private AuthRequestBuilder requestBuilder;

  private static final String BEARER_TOKEN = "Bearer %s";

  public OpenIdAuthProvider(
      WorkflowApplication application,
      Workflow workflow,
      OpenIdConnectAuthenticationPolicy authPolicy) {
    OpenIdConnectAuthenticationPolicyConfiguration configuration = authPolicy.getOidc();

    if (configuration.getOpenIdConnectAuthenticationProperties() != null) {
      this.requestBuilder =
          new OpenIdRequestBuilder(
              application, configuration.getOpenIdConnectAuthenticationProperties());
    } else if (configuration.getOpenIdConnectAuthenticationPolicySecret() != null) {
      throw new UnsupportedOperationException("Secrets are still not supported");
    }
  }

  @Override
  protected String authHeader(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    return String.format(
        BEARER_TOKEN, requestBuilder.build(workflow, task, model).validateAndGet().token());
  }
}
