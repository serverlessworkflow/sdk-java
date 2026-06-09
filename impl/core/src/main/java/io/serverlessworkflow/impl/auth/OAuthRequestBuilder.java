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

import static io.serverlessworkflow.impl.WorkflowUtils.concatURI;
import static io.serverlessworkflow.impl.auth.AuthUtils.AUTHORITY;

import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.OAuth2ConnectAuthenticationProperties;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.net.URI;
import java.util.Map;

class OAuthRequestBuilder
    extends AbstractAuthRequestBuilder<OAuth2ConnectAuthenticationProperties> {

  private static String DEFAULT_TOKEN_PATH = "oauth2/token";
  private static String DEFAULT_REVOCATION_PATH = "oauth2/revoke";
  private static String DEFAULT_INTROSPECTION_PATH = "oauth2/introspect";

  public OAuthRequestBuilder(WorkflowApplication application) {
    super(application);
  }

  @Override
  protected void authenticationURI(OAuth2ConnectAuthenticationProperties authenticationData) {
    OAuth2AuthenticationPropertiesEndpoints endpoints = authenticationData.getEndpoints();
    WorkflowValueResolver<URI> uri =
        WorkflowUtils.getURISupplier(application, authenticationData.getAuthority());
    String token = endpoints != null ? endpoints.getToken() : null;
    String revocation = endpoints != null ? endpoints.getRevocation() : null;
    String introspection = endpoints != null ? endpoints.getIntrospection() : null;
    requestBuilder
        .withUri(endpointResolver(uri, endpointPath(token, DEFAULT_TOKEN_PATH)))
        .withRevocationUri(endpointResolver(uri, endpointPath(revocation, DEFAULT_REVOCATION_PATH)))
        .withIntrospectionUri(
            endpointResolver(uri, endpointPath(introspection, DEFAULT_INTROSPECTION_PATH)));
  }

  @Override
  protected void authenticationURI(Map<String, Object> secret) {
    URI authority = URI.create((String) secret.get(AUTHORITY));
    Map<?, ?> endpoints =
        secret.get("endpoints") instanceof Map<?, ?> raw ? (Map<?, ?>) raw : Map.of();
    requestBuilder
        .withUri(staticUri(authority, endpoints, "token", DEFAULT_TOKEN_PATH))
        .withRevocationUri(staticUri(authority, endpoints, "revocation", DEFAULT_REVOCATION_PATH))
        .withIntrospectionUri(
            staticUri(authority, endpoints, "introspection", DEFAULT_INTROSPECTION_PATH));
  }

  private static String endpointPath(String path, String defaultPath) {
    return path != null ? path.replaceAll("^/", "") : defaultPath;
  }

  private WorkflowValueResolver<URI> endpointResolver(
      WorkflowValueResolver<URI> authority, String path) {
    return (w, t, m) -> concatURI(authority.apply(w, t, m), path);
  }

  private static WorkflowValueResolver<URI> staticUri(
      URI authority, Map<?, ?> endpoints, String key, String defaultPath) {
    String path =
        endpoints.get(key) instanceof String value ? endpointPath(value, defaultPath) : defaultPath;
    URI uri = concatURI(authority, path);
    return (w, t, m) -> uri;
  }
}
