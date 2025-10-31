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
package io.serverlessworkflow.impl.executors.http;

import io.serverlessworkflow.api.types.SecretBasedAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import jakarta.ws.rs.client.Invocation.Builder;

public abstract class AbstractAuthProvider implements AuthProvider {

  @Override
  public Builder build(
      Builder builder, WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    String authHeader = authHeader(workflow, task, model);
    task.authorization(authHeader);
    builder.header(AuthProviderFactory.AUTH_HEADER_NAME, authHeader);
    return builder;
  }

  protected final String checkSecret(
      Workflow workflow, SecretBasedAuthenticationPolicy secretPolicy) {
    String secretName = secretPolicy.getUse();
    return workflow.getUse().getSecrets().stream()
        .filter(s -> s.equals(secretName))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Secret " + secretName + " does not exist"));
  }

  protected final String find(WorkflowContext context, String secretName, String prop) {
    return context.definition().application().secretManager().secret(secretName).get(prop);
  }

  protected abstract String authHeader(
      WorkflowContext workflow, TaskContext task, WorkflowModel model);
}
