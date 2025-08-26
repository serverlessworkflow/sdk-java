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

import static io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient.ClientAuthentication.CLIENT_SECRET_POST;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

abstract class AbstractAuthRequestBuilder implements AuthRequestBuilder {

  protected final OAuth2AuthenticationData authenticationData;

  protected final WorkflowApplication application;

  private final List<Consumer<HttpRequestBuilder>> steps =
      List.of(
          this::requestEncoding,
          this::authenticationURI,
          this::audience,
          this::scope,
          this::authenticationMethod);

  protected AbstractAuthRequestBuilder(
      OAuth2AuthenticationData authenticationData, WorkflowApplication application) {
    this.authenticationData = authenticationData;
    this.application = application;
  }

  protected void audience(HttpRequestBuilder requestBuilder) {
    if (authenticationData.getAudiences() != null && !authenticationData.getAudiences().isEmpty()) {
      String audiences = String.join(" ", authenticationData.getAudiences());
      requestBuilder.addQueryParam("audience", audiences);
    }
  }

  protected void authenticationMethod(HttpRequestBuilder requestBuilder) {
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
    new ClientSecretBasic(authenticationData).execute(requestBuilder);
  }

  private void clientSecretPost(HttpRequestBuilder requestBuilder) {
    new ClientSecretPost(authenticationData).execute(requestBuilder);
  }

  private OAuth2AuthenticationDataClient.ClientAuthentication getClientAuthentication() {
    if (authenticationData.getClient() == null
        || authenticationData.getClient().getAuthentication() == null) {
      return CLIENT_SECRET_POST;
    }
    return authenticationData.getClient().getAuthentication();
  }

  @Override
  public AccessTokenProvider build(
      WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    HttpRequestBuilder requestBuilder = new HttpRequestBuilder(application);
    steps.forEach(step -> step.accept(requestBuilder));
    return new AccessTokenProvider(
        requestBuilder.build(workflow, task, model), task, authenticationData.getIssuers());
  }

  protected void scope(HttpRequestBuilder requestBuilder) {
    scope(requestBuilder, authenticationData.getScopes());
  }

  protected void scope(HttpRequestBuilder requestBuilder, List<String> scopesList) {
    if (scopesList == null || scopesList.isEmpty()) {
      return;
    }
    String scope =
        scopesList.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .flatMap(s -> Arrays.stream(s.split("\\s+")))
            .distinct()
            .collect(Collectors.joining(" "));

    if (!scope.isEmpty()) {
      requestBuilder.addQueryParam("scope", scope);
    }
  }

  void requestEncoding(HttpRequestBuilder requestBuilder) {
    if (authenticationData.getRequest() != null
        && authenticationData.getRequest().getEncoding() != null) {
      requestBuilder.addHeader(
          "Content-Type", authenticationData.getRequest().getEncoding().value());
    } else {
      requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }
  }

  protected abstract void authenticationURI(HttpRequestBuilder requestBuilder);
}
