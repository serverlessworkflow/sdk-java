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

import io.serverlessworkflow.api.types.BasicAuthenticationPolicy;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.util.Base64;

class BasicAuthProvider extends AbstractAuthProvider {

  private static final String BASIC_TOKEN = "Basic %s";
  private static final String USER_PASSWORD = "%s:%s";

  private final WorkflowValueResolver<String> userFilter;
  private final WorkflowValueResolver<String> passwordFilter;

  public BasicAuthProvider(
      WorkflowApplication app, Workflow workflow, BasicAuthenticationPolicy authPolicy) {
    if (authPolicy.getBasic().getBasicAuthenticationProperties() != null) {
      userFilter =
          WorkflowUtils.buildStringFilter(
              app, authPolicy.getBasic().getBasicAuthenticationProperties().getUsername());
      passwordFilter =
          WorkflowUtils.buildStringFilter(
              app, authPolicy.getBasic().getBasicAuthenticationProperties().getPassword());
    } else if (authPolicy.getBasic().getBasicAuthenticationPolicySecret() != null) {
      String secretName =
          checkSecret(workflow, authPolicy.getBasic().getBasicAuthenticationPolicySecret());
      userFilter = (w, t, m) -> find(w, secretName, "user");
      passwordFilter = (w, t, m) -> find(w, secretName, "password");
    } else {
      throw new IllegalStateException("Both secret and properties are null for authorization");
    }
  }

  @Override
  protected String authHeader(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    return String.format(
        BASIC_TOKEN,
        Base64.getEncoder()
            .encode(
                String.format(
                        USER_PASSWORD,
                        userFilter.apply(workflow, task, model),
                        passwordFilter.apply(workflow, task, model))
                    .getBytes()));
  }
}
