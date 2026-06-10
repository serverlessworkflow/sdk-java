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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;

public interface AccessTokenProvider {
  JWT validateAndGet(WorkflowContext workflow, TaskContext context, WorkflowModel model);

  /**
   * Introspects the given token against the configured introspection endpoint, as defined by <a
   * href="https://www.rfc-editor.org/rfc/rfc7662">RFC 7662</a>.
   *
   * <p>This is an optional capability. The default implementation throws {@link
   * UnsupportedOperationException}; providers backed by an introspection-capable OIDC client should
   * override it.
   *
   * @param tokenTypeHint optional {@code token_type_hint} (e.g. {@code access_token}), may be
   *     {@code null}
   */
  default TokenIntrospection introspect(
      WorkflowContext workflow,
      TaskContext context,
      WorkflowModel model,
      String token,
      String tokenTypeHint) {
    throw new UnsupportedOperationException(
        "Token introspection is not supported by this provider");
  }

  /**
   * Revokes the given token against the configured revocation endpoint, as defined by <a
   * href="https://www.rfc-editor.org/rfc/rfc7009">RFC 7009</a>.
   *
   * <p>This is an optional capability. The default implementation throws {@link
   * UnsupportedOperationException}; providers backed by a revocation-capable OIDC client should
   * override it.
   *
   * @param tokenTypeHint optional {@code token_type_hint} (e.g. {@code access_token}, {@code
   *     refresh_token}), may be {@code null}
   */
  default void revoke(
      WorkflowContext workflow,
      TaskContext context,
      WorkflowModel model,
      String token,
      String tokenTypeHint) {
    throw new UnsupportedOperationException("Token revocation is not supported by this provider");
  }
}
