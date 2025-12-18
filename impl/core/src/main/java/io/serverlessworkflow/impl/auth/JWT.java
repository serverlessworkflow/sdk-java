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
package io.serverlessworkflow.impl.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JWT {

  String token();

  List<String> audience();

  Map<String, Object> claims();

  <T> Optional<T> claim(String name, Class<T> type);

  Optional<Instant> expiresAt();

  Map<String, Object> header();

  Optional<Instant> issuedAt();

  Optional<String> issuer();

  Optional<String> subject();

  Optional<String> type();
}
