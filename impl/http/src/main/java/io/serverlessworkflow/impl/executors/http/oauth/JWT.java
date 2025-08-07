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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class JWT {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String token;
  private final Map<String, Object> claims;

  private JWT(String token, Map<String, Object> claims) {
    this.token = token;
    this.claims = claims;
  }

  public static JWT fromString(String token) throws JsonProcessingException {
    String[] parts = token.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT token format");
    }

    String payloadJson =
        new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    return new JWT(token, MAPPER.readValue(payloadJson, Map.class));
  }

  public String getToken() {
    return token;
  }

  public Object getClaim(String name) {
    return claims.get(name);
  }
}
