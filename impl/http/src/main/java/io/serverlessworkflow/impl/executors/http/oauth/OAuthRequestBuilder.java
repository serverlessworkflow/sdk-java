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

package io.serverlessworkflow.impl.executors.http.oauth;

import static io.serverlessworkflow.api.types.OAuth2AutenthicationDataClient.ClientAuthentication.*;

import io.serverlessworkflow.api.types.OAuth2AutenthicationData;
import io.serverlessworkflow.api.types.OAuth2AutenthicationDataClient;
import io.serverlessworkflow.api.types.OAuth2AuthenticationPropertiesEndpoints;
import io.serverlessworkflow.api.types.Oauth2;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class OAuthRequestBuilder {

  private final Oauth2 oauth2;

  private final OAuth2AutenthicationData authenticationData;

  private final WorkflowApplication application;

  private List<String> issuers;

  private final Map<String, String> defaults =
      Map.of(
          "endpoints.token", "oauth2/token",
          "endpoints.revocation", "oauth2/revoke",
          "endpoints.introspection", "oauth2/introspect");

  public OAuthRequestBuilder(WorkflowApplication application, Oauth2 oauth2) {
    this.oauth2 = oauth2;
    this.authenticationData =
        oauth2.getOAuth2ConnectAuthenticationProperties().getOAuth2AutenthicationData();
    this.application = application;
  }

  public AccessTokenProvider build(
      WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    HttpRequestBuilder requestBuilder = new HttpRequestBuilder(application);

    requestEncoding(requestBuilder);
    authenticationURI(requestBuilder);
    audience(requestBuilder);
    scope(requestBuilder);
    issuers();
    authenticationMethod(requestBuilder);

    return new AccessTokenProvider(requestBuilder.build(workflow, task, model), task, issuers);
  }

  private void authenticationMethod(HttpRequestBuilder requestBuilder) {
    switch (getClientAuthentication()) {
      case CLIENT_SECRET_BASIC:
        clientSecretBasic(requestBuilder);
      case CLIENT_SECRET_JWT:
        throw new UnsupportedOperationException("Client Secret JWT is not supported yet");
      case PRIVATE_KEY_JWT:
        throw new UnsupportedOperationException("Private Key JWT is not supported yet");
      default:
        clientSecretPost(requestBuilder);
    }
  }

  private void clientSecretBasic(HttpRequestBuilder requestBuilder) {
    new ClientSecretBasic(oauth2).execute(requestBuilder);
  }

  private void clientSecretPost(HttpRequestBuilder requestBuilder) {
    new ClientSecretPostStep(oauth2).execute(requestBuilder);
  }

  private OAuth2AutenthicationDataClient.ClientAuthentication getClientAuthentication() {
    if (authenticationData.getClient() == null
        || authenticationData.getClient().getAuthentication() == null) {
      return CLIENT_SECRET_POST;
    }
    return authenticationData.getClient().getAuthentication();
  }

  private void issuers() {
    issuers =
        oauth2
            .getOAuth2ConnectAuthenticationProperties()
            .getOAuth2AutenthicationData()
            .getIssuers();
  }

  public void audience(HttpRequestBuilder requestBuilder) {
    if (authenticationData.getAudiences() != null && !authenticationData.getAudiences().isEmpty()) {
      String audiences = String.join(" ", authenticationData.getAudiences());
      requestBuilder.addQueryParam("audience", audiences);
    }
  }

  private void scope(HttpRequestBuilder requestBuilder) {
    if (authenticationData.getScopes() != null && !authenticationData.getScopes().isEmpty()) {
      String scopes = String.join(" ", authenticationData.getScopes());
      requestBuilder.addQueryParam("scope", scopes);
    }
  }

  private void authenticationURI(HttpRequestBuilder requestBuilder) {
    OAuth2AuthenticationPropertiesEndpoints endpoints =
        oauth2
            .getOAuth2ConnectAuthenticationProperties()
            .getOAuth2ConnectAuthenticationProperties()
            .getEndpoints();

    String baseUri =
        oauth2
            .getOAuth2ConnectAuthenticationProperties()
            .getOAuth2AutenthicationData()
            .getAuthority()
            .getLiteralUri()
            .toString()
            .replaceAll("/$", "");
    String tokenPath = defaults.get("endpoints.token");
    if (endpoints != null && endpoints.getToken() != null) {
      tokenPath = endpoints.getToken().replaceAll("^/", "");
    }
    requestBuilder.withUri(URI.create(baseUri + "/" + tokenPath));
  }

  public void requestEncoding(HttpRequestBuilder requestBuilder) {
    if (authenticationData.getRequest() != null
        && authenticationData.getRequest().getEncoding() != null) {
      requestBuilder.addHeader(
          "Content-Type", authenticationData.getRequest().getEncoding().value());
    } else {
      requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }
  }
}
