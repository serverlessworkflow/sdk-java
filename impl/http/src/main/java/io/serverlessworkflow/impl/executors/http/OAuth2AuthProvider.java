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

import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicy;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPolicyConfiguration;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.AuthRequestBuilder;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.OAuthRequestBuilder;

public class OAuth2AuthProvider extends AbstractAuthProvider {

  private AuthRequestBuilder requestBuilder;

  public OAuth2AuthProvider(
      WorkflowApplication application, Workflow workflow, OAuth2AuthenticationPolicy authPolicy) {
    OAuth2AuthenticationPolicyConfiguration oauth2 = authPolicy.getOauth2();
    if (oauth2.getOAuth2ConnectAuthenticationProperties() != null) {
      this.requestBuilder =
          new OAuthRequestBuilder(application, oauth2.getOAuth2ConnectAuthenticationProperties());
    } else if (oauth2.getOAuth2AuthenticationPolicySecret() != null) {
      throw new UnsupportedOperationException("Secrets are still not supported");
    }
  }

  @Override
  protected String authParameter(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    return requestBuilder.build(workflow, task, model).validateAndGet().token();
  }

  @Override
  protected String authScheme() {
    return "Bearer";
  }
}
