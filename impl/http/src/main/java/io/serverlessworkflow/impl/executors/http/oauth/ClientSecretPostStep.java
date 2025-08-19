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

import static io.serverlessworkflow.api.types.OAuth2AutenthicationData.OAuth2AutenthicationDataGrant.CLIENT_CREDENTIALS;
import static io.serverlessworkflow.api.types.OAuth2AutenthicationData.OAuth2AutenthicationDataGrant.PASSWORD;

import io.serverlessworkflow.api.types.OAuth2AutenthicationData;
import io.serverlessworkflow.api.types.Oauth2;

class ClientSecretPostStep {
  private final Oauth2 oauth2;

  public ClientSecretPostStep(Oauth2 oauth2) {
    this.oauth2 = oauth2;
  }

  public void execute(HttpRequestBuilder requestBuilder) {
    OAuth2AutenthicationData authenticationData =
        oauth2.getOAuth2ConnectAuthenticationProperties().getOAuth2AutenthicationData();

    if (authenticationData.getGrant().equals(PASSWORD)) {
      password(requestBuilder, authenticationData);
    } else if (authenticationData.getGrant().equals(CLIENT_CREDENTIALS)) {
      clientCredentials(requestBuilder, authenticationData);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported grant type: " + authenticationData.getGrant());
    }
  }

  private void clientCredentials(
      HttpRequestBuilder requestBuilder, OAuth2AutenthicationData authenticationData) {
    if (authenticationData.getClient() == null
        || authenticationData.getClient().getId() == null
        || authenticationData.getClient().getSecret() == null) {
      throw new IllegalArgumentException(
          "Client ID and secret must be provided for client authentication");
    }

    requestBuilder
        .withGrantType(authenticationData.getGrant())
        .withRequestContentType(authenticationData.getRequest())
        .addQueryParam("client_id", authenticationData.getClient().getId())
        .addQueryParam("client_secret", authenticationData.getClient().getSecret());
  }

  private void password(
      HttpRequestBuilder requestBuilder, OAuth2AutenthicationData authenticationData) {
    if (authenticationData.getUsername() == null || authenticationData.getPassword() == null) {
      throw new IllegalArgumentException(
          "Username and password must be provided for password grant type");
    }
    if (authenticationData.getClient() == null
        || authenticationData.getClient().getId() == null
        || authenticationData.getClient().getSecret() == null) {
      throw new IllegalArgumentException(
          "Client ID and secret must be provided for client authentication");
    }

    requestBuilder
        .withGrantType(authenticationData.getGrant())
        .withRequestContentType(authenticationData.getRequest())
        .addQueryParam("client_id", authenticationData.getClient().getId())
        .addQueryParam("client_secret", authenticationData.getClient().getSecret())
        .addQueryParam("username", authenticationData.getUsername())
        .addQueryParam("password", authenticationData.getPassword());
  }
}
