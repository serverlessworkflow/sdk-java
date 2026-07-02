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

import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import io.serverlessworkflow.api.types.EndpointConfiguration;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.Use;
import io.serverlessworkflow.api.types.UseAuthentications;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.util.Optional;

public class DefaultAuthProviderFactory implements AuthProviderFactory {

  private static class DefaultAuthProviderFactoryHolder {
    private static final DefaultAuthProviderFactory instance = new DefaultAuthProviderFactory();
  }

  public static DefaultAuthProviderFactory factory() {
    return DefaultAuthProviderFactoryHolder.instance;
  }

  @Override
  public Optional<AuthProvider> getAuth(
      WorkflowDefinition definition, EndpointConfiguration configuration) {
    return configuration == null
        ? Optional.empty()
        : getAuth(definition, configuration.getAuthentication(), "GET");
  }

  @Override
  public Optional<AuthProvider> getAuth(
      WorkflowDefinition definition, ReferenceableAuthenticationPolicy auth, String method) {
    AuthenticationPolicyUnion policy = resolvePolicy(definition.workflow(), auth);
    return policy == null
        ? Optional.empty()
        : buildFromPolicy(definition.application(), definition.workflow(), policy, method);
  }

  public static AuthenticationPolicyUnion resolvePolicy(
      Workflow workflow, ReferenceableAuthenticationPolicy auth) {
    if (workflow == null) {
      throw new IllegalArgumentException(
          "workflow must not be null when resolving an authentication policy reference");
    }
    if (auth == null) {
      return null;
    }
    if (auth.getAuthenticationPolicyReference() != null) {
      String use = auth.getAuthenticationPolicyReference().getUse();
      Use useInfo = workflow.getUse();
      if (useInfo == null) {
        return null;
      }
      UseAuthentications authInfo = useInfo.getAuthentications();
      return authInfo == null ? null : authInfo.getAdditionalProperties().get(use);
    }
    return auth.getAuthenticationPolicy();
  }

  private Optional<AuthProvider> buildFromPolicy(
      WorkflowApplication app,
      Workflow workflow,
      AuthenticationPolicyUnion authenticationPolicy,
      String method) {
    if (authenticationPolicy.getBasicAuthenticationPolicy() != null) {
      return Optional.of(
          new BasicAuthProvider(
              app, workflow, authenticationPolicy.getBasicAuthenticationPolicy()));
    } else if (authenticationPolicy.getBearerAuthenticationPolicy() != null) {
      return Optional.of(
          new BearerAuthProvider(
              app, workflow, authenticationPolicy.getBearerAuthenticationPolicy()));
    } else if (authenticationPolicy.getDigestAuthenticationPolicy() != null) {
      return Optional.of(
          new DigestAuthProvider(
              app, workflow, authenticationPolicy.getDigestAuthenticationPolicy(), method));
    }
    return OAuthPolicyData.from(authenticationPolicy)
        .map(
            policyData ->
                policyData.scheme() == OAuthPolicyData.OAuthScheme.OPENID_CONNECT
                    ? new OpenIdAuthProvider(app, workflow, policyData)
                    : new OAuth2AuthProvider(app, workflow, policyData));
  }
}
