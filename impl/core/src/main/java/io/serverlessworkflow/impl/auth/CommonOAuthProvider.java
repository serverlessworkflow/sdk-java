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
package io.serverlessworkflow.impl.auth;

import static io.serverlessworkflow.impl.WorkflowUtils.checkSecret;
import static io.serverlessworkflow.impl.WorkflowUtils.loadFirst;
import static io.serverlessworkflow.impl.WorkflowUtils.secret;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.SecretBasedAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

abstract class CommonOAuthProvider implements AuthProvider {

  private final WorkflowValueResolver<AccessTokenProvider> tokenProvider;

  private static JWTConverter jwtConverter =
      loadFirst(JWTConverter.class)
          .orElseThrow(() -> new IllegalStateException("No JWTConverter implementation found"));

  private static AccessTokenProviderFactory accessTokenProviderFactory =
      loadFirst(AccessTokenProviderFactory.class)
          .orElseThrow(() -> new IllegalStateException("No JWTConverter implementation found"));

  protected CommonOAuthProvider(WorkflowValueResolver<AccessTokenProvider> tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public String content(WorkflowContext workflow, TaskContext task, WorkflowModel model, URI uri) {
    return tokenProvider.apply(workflow, task, model).validateAndGet(workflow, task, model).token();
  }

  @Override
  public String scheme() {
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
      return build(authenticationData, builder);
    } else if (secret != null) {
      return build(checkSecret(workflow, secret), builder);
    }
    throw new IllegalStateException("Both policy and secret are null");
  }

  private static WorkflowValueResolver<AccessTokenProvider> build(
      OAuth2AuthenticationData authenticationData, AuthRequestBuilder authBuilder) {
    AccessTokenProvider tokenProvider =
        accessTokenProviderFactory.build(
            authBuilder.apply(authenticationData), authenticationData.getIssuers(), jwtConverter);
    return (w, t, m) -> tokenProvider;
  }

  private static WorkflowValueResolver<AccessTokenProvider> build(
      String secretName, AuthRequestBuilder authBuilder) {
    return (w, t, m) -> {
      Map<String, Object> secret = secret(w, secretName);
      String issuers = (String) secret.get("issuers");
      return accessTokenProviderFactory.build(
          authBuilder.apply(secret),
          issuers != null ? Arrays.asList(issuers.split(",")) : null,
          jwtConverter);
    };
  }
}
