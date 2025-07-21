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
package io.serverlessworkflow.fluent.spec;

import java.util.function.Consumer;

public final class WorkflowBuilderConsumers {

  private WorkflowBuilderConsumers() {}

  public static Consumer<AuthenticationPolicyUnionBuilder> authBasic(
      final String username, final String password) {
    return auth -> auth.basic(b -> b.username(username).password(password));
  }

  public static Consumer<AuthenticationPolicyUnionBuilder> authBearer(final String token) {
    return auth -> auth.bearer(b -> b.token(token));
  }

  public static Consumer<AuthenticationPolicyUnionBuilder> authDigest(
      final String username, final String password) {
    return auth -> auth.digest(d -> d.username(username).password(password));
  }
}
