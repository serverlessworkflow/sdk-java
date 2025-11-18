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
package io.serverlessworkflow.fluent.func.dsl;

import io.serverlessworkflow.fluent.func.FuncCallHttpTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.dsl.BaseCallHttpSpec;
import io.serverlessworkflow.fluent.spec.spi.CallHttpTaskFluent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FuncCallHttpStep extends Step<FuncCallHttpStep, FuncCallHttpTaskBuilder>
    implements BaseCallHttpSpec<FuncCallHttpStep> {

  private final List<Consumer<CallHttpTaskFluent<?>>> steps = new ArrayList<>();

  private String name;

  public FuncCallHttpStep(String name) {
    this.name = name;
  }

  public FuncCallHttpStep() {}

  @Override
  public FuncCallHttpStep self() {
    return this;
  }

  protected void configure(FuncTaskItemListBuilder list, Consumer<FuncCallHttpTaskBuilder> post) {
    list.http(
        name,
        builder -> {
          this.accept(builder);
          post.accept(builder);
        });
  }

  @Override
  public List<Consumer<CallHttpTaskFluent<?>>> steps() {
    return steps;
  }
}
