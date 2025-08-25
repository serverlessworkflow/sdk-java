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
package io.serverlessworkflow.impl.http.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.http.jwt.JWT;
import io.serverlessworkflow.http.jwt.JWTConverter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class JacksonJWTConverter implements JWTConverter {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public JWT fromToken(String token) throws IllegalArgumentException {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("JWT token must not be null or blank");
    }

    String[] parts = token.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT token format");
    }
    try {
      String headerJson =
          new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

      Map<String, Object> header = MAPPER.readValue(headerJson, Map.class);
      Map<String, Object> claims = MAPPER.readValue(payloadJson, Map.class);

      return new JacksonJWTImpl(token, header, claims);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to parse JWT token payload: " + e.getMessage(), e);
    }
  }
}
