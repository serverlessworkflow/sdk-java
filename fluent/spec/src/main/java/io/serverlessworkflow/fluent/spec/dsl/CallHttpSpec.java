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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.fluent.spec.CallHttpTaskBuilder;
import io.serverlessworkflow.fluent.spec.configurers.CallHttpConfigurer;
import io.serverlessworkflow.fluent.spec.spi.CallHttpTaskFluent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CallHttpSpec implements BaseCallHttpSpec<CallHttpSpec>, CallHttpConfigurer {

  private final List<Consumer<CallHttpTaskFluent<?>>> steps = new ArrayList<>();

  public CallHttpSpec() {}

  @Override
  public CallHttpSpec self() {
    return this;
  }

  @Override
  public List<Consumer<CallHttpTaskFluent<?>>> steps() {
    return steps;
  }

  @Override
  public void accept(CallHttpTaskBuilder builder) {
    BaseCallHttpSpec.super.accept(builder);
  }
}
