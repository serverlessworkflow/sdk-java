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
import static io.serverlessworkflow.impl.WorkflowUtils.isValid;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.AUDIENCES;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.AUTHENTICATION;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.CLIENT;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.ENCODING;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.REQUEST;
import static io.serverlessworkflow.impl.executors.http.SecretKeys.SCOPES;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.OAuth2AuthenticationDataClient;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

abstract class AbstractAuthRequestBuilder<T extends OAuth2AuthenticationData>
    implements AuthRequestBuilder<T> {

  protected final WorkflowApplication application;
  protected final HttpRequestInfoBuilder requestBuilder = new HttpRequestInfoBuilder();

  public AbstractAuthRequestBuilder(WorkflowApplication application) {
    this.application = application;
  }

  @Override
  public HttpRequestInfo apply(T authenticationData) {
    requestEncoding(authenticationData);
    authenticationURI(authenticationData);
    audience(authenticationData);
    scope(authenticationData);
    authenticationMethod(authenticationData);
    return requestBuilder.build();
  }

  @Override
  public HttpRequestInfo apply(Map<String, Object> secret) {
    requestEncoding(secret);
    authenticationURI(secret);
    audience(secret);
    scope(secret);
    authenticationMethod(secret);
    return requestBuilder.build();
  }

  protected void audience(T authenticationData) {
    if (authenticationData.getAudiences() != null && !authenticationData.getAudiences().isEmpty()) {
      String audiences = String.join(" ", authenticationData.getAudiences());
      requestBuilder.addQueryParam(
          "audience", WorkflowUtils.buildStringFilter(application, audiences));
    }
  }

  protected void audience(Map<String, Object> secret) {
    String audiences = (String) secret.get(AUDIENCES);
    if (isValid(audiences)) {
      requestBuilder.addQueryParam("audience", audiences);
    }
  }

  protected void authenticationMethod(T authenticationData) {
    ClientSecretHandler secretHandler;
    switch (getClientAuthentication(authenticationData)) {
      case CLIENT_SECRET_BASIC:
        secretHandler = new ClientSecretBasic(application, requestBuilder);
      case CLIENT_SECRET_JWT:
        throw new UnsupportedOperationException("Client Secret JWT is not supported yet");
      case PRIVATE_KEY_JWT:
        throw new UnsupportedOperationException("Private Key JWT is not supported yet");
      default:
        secretHandler = new ClientSecretPost(application, requestBuilder);
    }
    secretHandler.accept(authenticationData);
  }

  protected void authenticationMethod(Map<String, Object> secret) {
    Map<String, Object> client = (Map<String, Object>) secret.get(CLIENT);
    ClientSecretHandler secretHandler;
    String auth = (String) client.get(AUTHENTICATION);
    if (auth == null) {
      secretHandler = new ClientSecretPost(application, requestBuilder);
    } else {
      switch (auth) {
        case "client_secret_basic":
          secretHandler = new ClientSecretBasic(application, requestBuilder);
          break;
        default:
        case "client_secret_post":
          secretHandler = new ClientSecretPost(application, requestBuilder);
          break;
        case "private_key_jwt":
          throw new UnsupportedOperationException("Private Key JWT is not supported yet");
        case "client_secret_jwt":
          throw new UnsupportedOperationException("Client Secret JWT is not supported yet");
      }
    }
    secretHandler.accept(secret);
  }

  private OAuth2AuthenticationDataClient.ClientAuthentication getClientAuthentication(
      OAuth2AuthenticationData authenticationData) {
    return authenticationData.getClient() == null
            || authenticationData.getClient().getAuthentication() == null
        ? CLIENT_SECRET_POST
        : authenticationData.getClient().getAuthentication();
  }

  protected void scope(T authenticationData) {
    scope(authenticationData.getScopes());
  }

  protected void scope(List<String> scopesList) {
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
      requestBuilder.addQueryParam("scope", WorkflowUtils.buildStringFilter(application, scope));
    }
  }

  protected void scope(Map<String, Object> secret) {
    String scopes = (String) secret.get(SCOPES);
    if (isValid(scopes)) {
      requestBuilder.addQueryParam("scope", scopes);
    }
  }

  void requestEncoding(T authenticationData) {
    requestBuilder.withContentType(authenticationData.getRequest());
  }

  void requestEncoding(Map<String, Object> secret) {
    Map<String, Object> request = (Map<String, Object>) secret.get(REQUEST);
    String encoding = (String) request.get(ENCODING);
    if (isValid(encoding)) {
      requestBuilder.addHeader("Content-Type", encoding);
    }
  }

  protected abstract void authenticationURI(T authenticationData);

  protected abstract void authenticationURI(Map<String, Object> secret);
}
