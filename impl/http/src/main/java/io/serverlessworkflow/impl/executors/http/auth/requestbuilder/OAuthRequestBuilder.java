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

import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.Oauth2;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.net.URI;
import java.util.Map;

public class OAuthRequestBuilder extends AbstractAuthRequestBuilder {

  private final Oauth2 oauth2;

  private final Map<String, String> defaults =
      Map.of(
          "endpoints.token", "oauth2/token",
          "endpoints.revocation", "oauth2/revoke",
          "endpoints.introspection", "oauth2/introspect");

  public OAuthRequestBuilder(WorkflowApplication application, Oauth2 oauth2) {
    super(
        oauth2.getOAuth2ConnectAuthenticationProperties().getOAuth2AuthenticationData(),
        application);
    this.oauth2 = oauth2;
  }

  @Override
  protected void authenticationURI(HttpRequestBuilder requestBuilder) {
    OAuth2AuthenticationPropertiesEndpoints endpoints =
        oauth2
            .getOAuth2ConnectAuthenticationProperties()
            .getOAuth2ConnectAuthenticationProperties()
            .getEndpoints();

    String baseUri =
        authenticationData.getAuthority().getLiteralUri().toString().replaceAll("/$", "");
    String tokenPath = defaults.get("endpoints.token");
    if (endpoints != null && endpoints.getToken() != null) {
      tokenPath = endpoints.getToken().replaceAll("^/", "");
    }
    requestBuilder.withUri(URI.create(baseUri + "/" + tokenPath));
  }
}
