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

import static io.serverlessworkflow.impl.executors.http.SecretKeys.CLIENT;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.GRANT;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.ID;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.PASSWORD;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.SECRET;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.USER;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Map;

class ClientSecretPost extends ClientSecretHandler {

  protected ClientSecretPost(
      WorkflowApplication application, HttpRequestInfoBuilder requestBuilder) {
    super(application, requestBuilder);
  }

  @Override
  protected void clientCredentials(OAuth2AuthenticationData authenticationData) {
    requestBuilder
        .withGrantType(authenticationData.getGrant().value())
        .addQueryParam(
            "client_id",
            WorkflowUtils.buildStringFilter(application, authenticationData.getClient().getId()))
        .addQueryParam(
            "client_secret",
            WorkflowUtils.buildStringFilter(
                application, authenticationData.getClient().getSecret()));
  }

  @Override
  protected void password(OAuth2AuthenticationData authenticationData) {
    clientCredentials(authenticationData);
    requestBuilder
        .addQueryParam(
            "username",
            WorkflowUtils.buildStringFilter(application, authenticationData.getUsername()))
        .addQueryParam(
            "password",
            WorkflowUtils.buildStringFilter(application, authenticationData.getPassword()));
  }

  @Override
  protected void clientCredentials(Map<String, Object> secret) {
    Map<String, Object> client = (Map<String, Object>) secret.get(CLIENT);
    requestBuilder
        .withGrantType((String) secret.get(GRANT))
        .addQueryParam("client_id", (String) client.get(ID))
        .addQueryParam("client_secret", (String) client.get(SECRET));
  }

  @Override
  protected void password(Map<String, Object> secret) {
    clientCredentials(secret);
    requestBuilder.addQueryParam("username", (String) secret.get(USER));
    requestBuilder.addQueryParam("password", (String) secret.get(PASSWORD));
  }
}
