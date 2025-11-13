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
package io.serverlessworkflow.impl.executors.http.auth.requestbuilder;

import static io.serverlessworkflow.impl.WorkflowUtils.concatURI;

import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.OAuth2ConnectAuthenticationProperties;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.net.URI;
import java.util.Map;

public class OAuthRequestBuilder
    extends AbstractAuthRequestBuilder<OAuth2ConnectAuthenticationProperties> {

  private static String DEFAULT_TOKEN_PATH = "oauth2/token";

  public OAuthRequestBuilder(WorkflowApplication application) {
    super(application);
  }

  // TODO handle revocation and introspection path
  // private static String DEFAULT_REVOCATION_PATH = "oauth2/revoke";
  // private static String DEFAULT_INTROSPECTION_PATH = "oauth2/introspect";

  @Override
  protected void authenticationURI(
      HttpRequestBuilder requestBuilder, OAuth2ConnectAuthenticationProperties authenticationData) {
    // TODO support URI template
    OAuth2AuthenticationPropertiesEndpoints endpoints = authenticationData.getEndpoints();
    WorkflowValueResolver<URI> uri =
        WorkflowUtils.getURISupplier(application, authenticationData.getAuthority());
    requestBuilder.withUri(
        (w, t, m) ->
            concatURI(
                uri.apply(w, t, m),
                endpoints != null && endpoints.getToken() != null
                    ? endpoints.getToken().replaceAll("^/", "")
                    : DEFAULT_TOKEN_PATH));
  }

  @Override
  protected void authenticationURI(HttpRequestBuilder requestBuilder, Map<String, Object> secret) {
    String tokenPath =
        secret.get("endpoints") instanceof Map endpoints ? (String) endpoints.get("token") : null;
    URI uri =
        concatURI(
            URI.create((String) secret.get("authority")),
            tokenPath == null ? DEFAULT_TOKEN_PATH : tokenPath);
    requestBuilder.withUri((w, t, m) -> uri);
  }
}
