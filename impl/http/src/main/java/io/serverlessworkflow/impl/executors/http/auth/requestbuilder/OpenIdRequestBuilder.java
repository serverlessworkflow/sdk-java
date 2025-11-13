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

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenIdRequestBuilder extends AbstractAuthRequestBuilder<OAuth2AuthenticationData> {

  public OpenIdRequestBuilder(WorkflowApplication application) {
    super(application);
  }

  @Override
  protected void authenticationURI(
      HttpRequestBuilder requestBuilder, OAuth2AuthenticationData authenticationData) {
    requestBuilder.withUri(
        WorkflowUtils.getURISupplier(application, authenticationData.getAuthority()));
  }

  @Override
  protected void scope(
      HttpRequestBuilder requestBuilder, OAuth2AuthenticationData authenticationData) {
    List<String> scopesList = new ArrayList<>(authenticationData.getScopes());
    scopesList.add("openid");
    scope(requestBuilder, scopesList);
  }

  @Override
  protected void authenticationURI(HttpRequestBuilder requestBuilder, Map<String, Object> secret) {
    URI uri = URI.create((String) secret.get("authority"));
    requestBuilder.withUri((w, t, m) -> uri);
  }
}
