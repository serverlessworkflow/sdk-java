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

import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.util.function.Function;

public class FuncCallTaskBuilder extends TaskBaseBuilder<FuncCallTaskBuilder>
    implements FuncTaskTransformations<FuncCallTaskBuilder>,
        ConditionalTaskBuilder<FuncCallTaskBuilder> {

  private CallTaskJava callTaskJava;

  FuncCallTaskBuilder() {
    callTaskJava = new CallTaskJava(new CallJava() {});
    super.setTask(callTaskJava.getCallJava());
  }

  @Override
  protected FuncCallTaskBuilder self() {
    return this;
  }

  public <T, V> FuncCallTaskBuilder function(Function<T, V> function) {
    return function(function, null);
  }

  public <T, V> FuncCallTaskBuilder function(Function<T, V> function, Class<T> argClass) {
    this.callTaskJava = new CallTaskJava(CallJava.function(function, argClass));
    super.setTask(this.callTaskJava.getCallJava());
    return this;
  }

  public CallTaskJava build() {
    return this.callTaskJava;
  }
}
