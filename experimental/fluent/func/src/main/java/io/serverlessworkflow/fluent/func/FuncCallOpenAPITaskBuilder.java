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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.api.types.CallOpenAPI;
import io.serverlessworkflow.api.types.OpenAPIArguments;
import io.serverlessworkflow.api.types.WithOpenAPIParameters;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import io.serverlessworkflow.fluent.spec.spi.CallOpenAPITaskFluent;

public class FuncCallOpenAPITaskBuilder extends TaskBaseBuilder<FuncCallOpenAPITaskBuilder>
    implements CallOpenAPITaskFluent<FuncCallOpenAPITaskBuilder>,
        FuncTaskTransformations<FuncCallOpenAPITaskBuilder>,
        ConditionalTaskBuilder<FuncCallOpenAPITaskBuilder> {

  FuncCallOpenAPITaskBuilder() {
    final CallOpenAPI callOpenAPI = new CallOpenAPI();
    callOpenAPI.setWith(new OpenAPIArguments().withParameters(new WithOpenAPIParameters()));
    super.setTask(callOpenAPI);
  }

  @Override
  public FuncCallOpenAPITaskBuilder self() {
    return this;
  }
}
