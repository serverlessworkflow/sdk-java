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
package io.serverlessworkflow.impl.executors.http.oauth.jackson;

import io.serverlessworkflow.impl.executors.http.oauth.JWT;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JacksonJWTImpl implements JWT {

  private final Map<String, Object> header;
  private final Map<String, Object> claims;

  private final String token;

  JacksonJWTImpl(String token, Map<String, Object> header, Map<String, Object> claims) {
    this.token = Objects.requireNonNull(token, "token");
    this.header = Collections.unmodifiableMap(Objects.requireNonNull(header, "header"));
    this.claims = Collections.unmodifiableMap(Objects.requireNonNull(claims, "claims"));
  }

  @Override
  public String token() {
    return token;
  }

  @Override
  public Map<String, Object> header() {
    return header;
  }

  @Override
  public Map<String, Object> claims() {
    return claims;
  }

  @Override
  public <T> Optional<T> claim(String name, Class<T> type) {
    if (name == null || type == null) return Optional.empty();
    Object value = claims.get(name);
    return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
  }

  @Override
  public Optional<String> issuer() {
    return Optional.ofNullable(asString(claims.get("iss")));
  }

  @Override
  public Optional<String> subject() {
    return Optional.ofNullable(asString(claims.get("sub")));
  }

  @Override
  public List<String> audience() {
    Object aud = claims.get("aud");
    if (aud == null) {
      return List.of();
    } else if (aud instanceof String asString) {
      return List.of(asString);
    } else if (aud instanceof String[] asStringArray) {
      return Arrays.asList(asStringArray);
    }
    return ((Collection<?>) aud).stream().map(String::valueOf).toList();
  }

  @Override
  public Optional<Instant> issuedAt() {
    return Optional.ofNullable(toInstant(claims.get("iat")));
  }

  @Override
  public Optional<Instant> expiresAt() {
    return Optional.ofNullable(toInstant(claims.get("exp")));
  }

  @Override
  public Optional<String> type() {
    return header.containsKey("typ")
        ? Optional.of(asString(header.get("typ")))
        : Optional.ofNullable(asString(claims.get("typ")));
  }

  private static Instant toInstant(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Instant i) {
      return i;
    }
    if (v instanceof Number n) {
      return Instant.ofEpochSecond(n.longValue());
    }
    if (v instanceof String s) {
      try {
        long sec = Long.parseLong(s.trim());
        return Instant.ofEpochSecond(sec);
      } catch (NumberFormatException ignored) {
        try {
          return Instant.parse(s.trim());
        } catch (Exception e) {
          throw new IllegalArgumentException("Cannot parse time claim: " + s, e);
        }
      }
    }
    throw new IllegalArgumentException("Unsupported time claim type: " + v.getClass());
  }

  private static String asString(Object v) {
    return v == null ? null : String.valueOf(v).trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JacksonJWTImpl that)) return false;
    return Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token);
  }
}
