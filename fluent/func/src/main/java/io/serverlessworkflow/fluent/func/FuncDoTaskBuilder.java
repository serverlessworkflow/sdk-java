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

import io.serverlessworkflow.fluent.spec.BaseDoTaskBuilder;

public class FuncDoTaskBuilder extends BaseDoTaskBuilder<FuncDoTaskBuilder, FuncTaskItemListBuilder>
    implements FuncTransformations<FuncDoTaskBuilder>,
        DelegatingFuncDoTaskFluent<FuncDoTaskBuilder> {

  protected FuncDoTaskBuilder(FuncTaskItemListBuilder listBuilder) {
    super(listBuilder);
  }

  FuncDoTaskBuilder() {
    super(new FuncTaskItemListBuilder());
  }

  @Override
  public FuncDoTaskBuilder self() {
    return this;
  }

  @Override
  public FuncDoTaskFluent<FuncTaskItemListBuilder> funcInternalDelegate() {
    return internalDelegate();
  }
}
