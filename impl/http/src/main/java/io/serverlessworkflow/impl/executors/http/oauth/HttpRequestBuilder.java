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

import static io.serverlessworkflow.api.types.OAuth2TokenRequest.Oauth2TokenRequestEncoding;
import static io.serverlessworkflow.api.types.OAuth2TokenRequest.Oauth2TokenRequestEncoding.APPLICATION_X_WWW_FORM_URLENCODED;

import io.serverlessworkflow.api.types.OAuth2AutenthicationData;
import io.serverlessworkflow.api.types.OAuth2TokenRequest;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class HttpRequestBuilder {

  private final Map<String, WorkflowValueResolver<String>> headers;

  private final Map<String, WorkflowValueResolver<String>> queryParams;

  private final WorkflowApplication app;

  private URI uri;

  private OAuth2AutenthicationData.OAuth2AutenthicationDataGrant grantType;

  private Oauth2TokenRequestEncoding requestContentType = APPLICATION_X_WWW_FORM_URLENCODED;

  HttpRequestBuilder(WorkflowApplication app) {
    this.app = app;
    headers = new HashMap<>();
    queryParams = new HashMap<>();
  }

  HttpRequestBuilder addHeader(String key, String token) {
    headers.put(key, WorkflowUtils.buildStringFilter(app, token));
    return this;
  }

  HttpRequestBuilder addQueryParam(String key, String token) {
    queryParams.put(key, WorkflowUtils.buildStringFilter(app, token));
    return this;
  }

  HttpRequestBuilder withUri(URI uri) {
    this.uri = uri;
    return this;
  }

  HttpRequestBuilder withRequestContentType(OAuth2TokenRequest oAuth2TokenRequest) {
    if (oAuth2TokenRequest != null) {
      this.requestContentType = oAuth2TokenRequest.getEncoding();
    }
    return this;
  }

  HttpRequestBuilder withGrantType(
      OAuth2AutenthicationData.OAuth2AutenthicationDataGrant grantType) {
    this.grantType = grantType;
    return this;
  }

  InvocationHolder build(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    validate();

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(uri);

    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);

    builder.header("grant_type", grantType.name().toLowerCase());
    builder.header("User-Agent", "OAuth2-Client-Credentials/1.0");
    builder.header("Accept", MediaType.APPLICATION_JSON);
    builder.header("Cache-Control", "no-cache");

    for (var entry : headers.entrySet()) {
      String headerValue = entry.getValue().apply(workflow, task, model);
      if (headerValue != null) {
        builder.header(entry.getKey(), headerValue);
      }
    }

    Entity<?> entity;
    if (requestContentType.equals(APPLICATION_X_WWW_FORM_URLENCODED)) {
      Form form = new Form();
      form.param("grant_type", grantType.value());
      queryParams.forEach(
          (key, value) -> {
            String v = value.apply(workflow, task, model);
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8);
            form.param(encodedKey, encodedValue);
          });
      entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED);
    } else {
      Map<String, Object> jsonData = new HashMap<>();
      jsonData.put("grant_type", grantType.value());
      queryParams.forEach(
          (key, value) -> {
            String v = value.apply(workflow, task, model);
            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8);
            jsonData.put(encodedKey, encodedValue);
          });
      entity = Entity.entity(jsonData, MediaType.APPLICATION_JSON);
    }

    return new InvocationHolder(client, () -> builder.post(entity));
  }

  private void validate() {
    Objects.requireNonNull(uri, "URI must be set before building the request");
    Objects.requireNonNull(grantType, "Grant type must be set before building the request");
  }
}
