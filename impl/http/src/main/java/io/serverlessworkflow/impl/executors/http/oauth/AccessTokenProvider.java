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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.impl.TaskContext;
import java.net.http.HttpRequest;
import java.util.List;

public class AccessTokenProvider {

  private final TokenResponseHandler tokenResponseHandler = new TokenResponseHandler();

  private final TaskContext context;
  private final List<String> issuers;
  private final HttpRequest requestBuilder;

  public AccessTokenProvider(
      HttpRequest requestBuilder, TaskContext context, List<String> issuers) {
    this.requestBuilder = requestBuilder;
    this.issuers = issuers;
    this.context = context;
  }

  public JWT validateAndGet() {
    JsonNode token = tokenResponseHandler.apply(requestBuilder, context);
    JWT jwt;
    try {
      jwt = JWT.fromString(token.get("access_token").asText());
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JWT token: " + e.getMessage(), e);
    }
    if (!(issuers == null || issuers.isEmpty())) {
      String tokenIssuer = (String) jwt.getClaim("iss");
      if (tokenIssuer == null || tokenIssuer.isEmpty() || !issuers.contains(tokenIssuer)) {
        throw new RuntimeException("Token issuer is not valid: " + tokenIssuer);
      }
    }
    return jwt;
  }
}
