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

import io.serverlessworkflow.http.jwt.JWT;
import java.util.Map;

public class JacksonJWTImpl implements JWT {

  private final Map<String, Object> claims;
  private final String token;

  JacksonJWTImpl(String token, Map<String, Object> claims) {
    this.token = token;
    this.claims = claims;
  }

  @Override
  public String getToken() {
    return token;
  }

  @Override
  public Object getClaim(String name) {
    if (claims == null || claims.isEmpty()) {
      return null;
    }
    return claims.get(name);
  }
}
