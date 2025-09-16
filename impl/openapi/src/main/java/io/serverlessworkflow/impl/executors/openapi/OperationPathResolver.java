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
package io.serverlessworkflow.impl.executors.openapi;

import jakarta.ws.rs.core.UriBuilder;
import java.util.Map;

public class OperationPathResolver {

  private final String path;
  private final Map<String, Object> args;

  public OperationPathResolver(String path, Map<String, Object> args) {
    this.path = path;
    this.args = args;
  }

  public OpenAPIExecutor.TargetSupplier passPathParams() {
    return (workflow, taskContext, input) ->
        UriBuilder.fromUri(path).resolveTemplates(args, false).build();
  }
}
