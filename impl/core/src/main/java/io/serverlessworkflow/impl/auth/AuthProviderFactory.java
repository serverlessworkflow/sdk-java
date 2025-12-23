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
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.util.Optional;

public class AuthProviderFactory {

  private AuthProviderFactory() {}

  public static Optional<AuthProvider> getAuth(
      WorkflowDefinition definition, EndpointConfiguration configuration) {
    return configuration == null
        ? Optional.empty()
        : getAuth(definition, configuration.getAuthentication(), "GET");
  }

  public static Optional<AuthProvider> getAuth(
      WorkflowDefinition definition, ReferenceableAuthenticationPolicy auth, String method) {
    if (auth == null) {
      return Optional.empty();
    }
    if (auth.getAuthenticationPolicyReference() != null) {
      return buildFromReference(
          definition.application(),
          definition.workflow(),
          auth.getAuthenticationPolicyReference().getUse(),
          method);
    } else if (auth.getAuthenticationPolicy() != null) {
      return buildFromPolicy(
          definition.application(), definition.workflow(), auth.getAuthenticationPolicy(), method);
    }
    return Optional.empty();
  }

  private static Optional<AuthProvider> buildFromReference(
      WorkflowApplication app, Workflow workflow, String use, String method) {
    return workflow.getUse().getAuthentications().getAdditionalProperties().entrySet().stream()
        .filter(s -> s.getKey().equals(use))
        .findAny()
        .flatMap(e -> buildFromPolicy(app, workflow, e.getValue(), method));
  }

  private static Optional<AuthProvider> buildFromPolicy(
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
    } else if (authenticationPolicy.getOAuth2AuthenticationPolicy() != null) {
      return Optional.of(
          new OAuth2AuthProvider(
              app, workflow, authenticationPolicy.getOAuth2AuthenticationPolicy()));
    } else if (authenticationPolicy.getOpenIdConnectAuthenticationPolicy() != null) {
      return Optional.of(
          new OpenIdAuthProvider(
              app, workflow, authenticationPolicy.getOpenIdConnectAuthenticationPolicy()));
    }

    return Optional.empty();
  }
}
