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

import io.serverlessworkflow.api.types.AuthenticationPolicyUnion;
import io.serverlessworkflow.api.types.EndpointConfiguration;
import io.serverlessworkflow.api.types.ReferenceableAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.Optional;

class AuthProviderFactory {

  private AuthProviderFactory() {}

  static final String AUTH_HEADER_NAME = "Authorization";

  public static Optional<AuthProvider> getAuth(
      WorkflowApplication app, Workflow workflow, EndpointConfiguration endpointConfiguration) {
    if (endpointConfiguration == null) {
      return Optional.empty();
    }
    ReferenceableAuthenticationPolicy auth = endpointConfiguration.getAuthentication();
    if (auth == null) {
      return Optional.empty();
    }
    if (auth.getAuthenticationPolicyReference() != null) {
      return buildFromReference(app, workflow, auth.getAuthenticationPolicyReference().getUse());
    } else if (auth.getAuthenticationPolicy() != null) {
      return buildFromPolicy(app, workflow, auth.getAuthenticationPolicy());
    }
    return Optional.empty();
  }

  private static Optional<AuthProvider> buildFromReference(
      WorkflowApplication app, Workflow workflow, String use) {
    return workflow.getUse().getAuthentications().getAdditionalProperties().entrySet().stream()
        .filter(s -> s.getKey().equals(use))
        .findAny()
        .flatMap(e -> buildFromPolicy(app, workflow, e.getValue()));
  }

  private static Optional<AuthProvider> buildFromPolicy(
      WorkflowApplication app, Workflow workflow, AuthenticationPolicyUnion authenticationPolicy) {
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
              app, workflow, authenticationPolicy.getDigestAuthenticationPolicy()));
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
