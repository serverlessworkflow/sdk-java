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

import static io.serverlessworkflow.impl.WorkflowUtils.secret;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.util.Arrays;
import java.util.Map;

public class AccessTokenProviderFactory {

  private AccessTokenProviderFactory() {}

  public static WorkflowValueResolver<AccessTokenProvider> build(
      OAuth2AuthenticationData authenticationData, AuthRequestBuilder authBuilder) {
    HttpRequestBuilder httpBuilder = new HttpRequestBuilder();
    authBuilder.accept(httpBuilder, authenticationData);
    AccessTokenProvider tokenProvider =
        new AccessTokenProvider(httpBuilder, authenticationData.getIssuers());
    return (w, t, m) -> tokenProvider;
  }

  public static WorkflowValueResolver<AccessTokenProvider> build(
      String secretName, AuthRequestBuilder authBuilder) {
    HttpRequestBuilder httpBuilder = new HttpRequestBuilder();
    return (w, t, m) -> {
      Map<String, Object> secret = secret(w, secretName);
      authBuilder.accept(httpBuilder, secret);
      String issuers = (String) secret.get("issuers");
      return new AccessTokenProvider(
          httpBuilder, issuers != null ? Arrays.asList(issuers.split(",")) : null);
    };
  }
}
