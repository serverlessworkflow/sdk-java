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

import static io.serverlessworkflow.impl.WorkflowUtils.checkSecret;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.SecretBasedAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.AccessTokenProvider;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.AccessTokenProviderFactory;
import io.serverlessworkflow.impl.executors.http.auth.requestbuilder.AuthRequestBuilder;
import java.util.Map;

abstract class CommonOAuthProvider extends AbstractAuthProvider {

  private final WorkflowValueResolver<AccessTokenProvider> tokenProvider;

  protected CommonOAuthProvider(WorkflowValueResolver<AccessTokenProvider> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  protected String authParameter(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    return tokenProvider.apply(workflow, task, model).validateAndGet(workflow, task, model).token();
  }

  @Override
  protected String authScheme() {
    return "Bearer";
  }

  protected static OAuth2AuthenticationData fillFromMap(
      OAuth2AuthenticationData data, Map<String, Object> secretMap) {
    return data;
  }

  protected static WorkflowValueResolver<AccessTokenProvider> accessToken(
      Workflow workflow,
      OAuth2AuthenticationData authenticationData,
      SecretBasedAuthenticationPolicy secret,
      AuthRequestBuilder<?> builder) {
    if (authenticationData != null) {
      return AccessTokenProviderFactory.build(authenticationData, builder);
    } else if (secret != null) {
      return AccessTokenProviderFactory.build(checkSecret(workflow, secret), builder);
    }
    throw new IllegalStateException("Both policy and secret are null");
  }
}
