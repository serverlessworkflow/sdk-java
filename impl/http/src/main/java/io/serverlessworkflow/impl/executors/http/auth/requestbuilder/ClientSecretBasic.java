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

import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.CLIENT;
import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.GRANT;
import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.ID;
import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.PASSWORD;
import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.SECRET;
import static io.serverlessworkflow.impl.executors.http.auth.requestbuilder.SecretKeys.USER;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Base64;
import java.util.Map;

class ClientSecretBasic extends ClientSecretHandler {

  protected ClientSecretBasic(WorkflowApplication application) {
    super(application);
  }

  @Override
  protected void clientCredentials(
      HttpRequestBuilder requestBuilder, OAuth2AuthenticationData authenticationData) {
    requestBuilder
        .addHeader("Authorization", "Basic " + encodedAuth(authenticationData))
        .withGrantType(authenticationData.getGrant().value());
  }

  @Override
  protected void password(
      HttpRequestBuilder requestBuilder, OAuth2AuthenticationData authenticationData) {
    clientCredentials(requestBuilder, authenticationData);
    requestBuilder
        .addQueryParam(
            "username",
            WorkflowUtils.buildStringFilter(application, authenticationData.getUsername()))
        .addQueryParam(
            "password",
            WorkflowUtils.buildStringFilter(application, authenticationData.getPassword()));
  }

  @Override
  protected void clientCredentials(HttpRequestBuilder requestBuilder, Map<String, Object> secret) {
    requestBuilder
        .withGrantType((String) secret.get(GRANT))
        .addHeader("Authorization", "Basic " + encodedAuth(secret));
  }

  @Override
  protected void password(HttpRequestBuilder requestBuilder, Map<String, Object> secret) {
    clientCredentials(requestBuilder, secret);
    requestBuilder
        .addQueryParam("username", (String) secret.get(USER))
        .addQueryParam("password", (String) secret.get(PASSWORD));
  }

  private String encodedAuth(Map<String, Object> secret) {
    Map<String, Object> client = (Map<String, Object>) secret.get(CLIENT);
    return encodedAuth((String) client.get(ID), (String) client.get(SECRET));
  }

  private String encodedAuth(OAuth2AuthenticationData authenticationData) {
    return encodedAuth(
        authenticationData.getClient().getId(), authenticationData.getClient().getSecret());
  }

  private String encodedAuth(String id, String secret) {
    return Base64.getEncoder().encodeToString((id + ":" + secret).getBytes());
  }
}
