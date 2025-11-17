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
import io.serverlessworkflow.impl.executors.http.auth.jwt.JWTConverter;
import java.util.Arrays;
import java.util.Map;
import java.util.ServiceLoader;

public class AccessTokenProviderFactory {

  private AccessTokenProviderFactory() {}

  private static JWTConverter jwtConverter =
      ServiceLoader.load(JWTConverter.class)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No JWTConverter implementation found"));

  public static WorkflowValueResolver<AccessTokenProvider> build(
      OAuth2AuthenticationData authenticationData, AuthRequestBuilder authBuilder) {
    AccessTokenProvider tokenProvider =
        new AccessTokenProvider(
            authBuilder.apply(authenticationData), authenticationData.getIssuers(), jwtConverter);
    return (w, t, m) -> tokenProvider;
  }

  public static WorkflowValueResolver<AccessTokenProvider> build(
      String secretName, AuthRequestBuilder authBuilder) {
    return (w, t, m) -> {
      Map<String, Object> secret = secret(w, secretName);
      String issuers = (String) secret.get("issuers");
      return new AccessTokenProvider(
          authBuilder.apply(secret),
          issuers != null ? Arrays.asList(issuers.split(",")) : null,
          jwtConverter);
    };
  }
}
