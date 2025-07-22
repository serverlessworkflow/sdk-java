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
package io.serverlessworkflow.fluent.java;

import io.serverlessworkflow.api.types.CallJava;
import io.serverlessworkflow.api.types.CallTaskJava;
import io.serverlessworkflow.fluent.standard.TaskBaseBuilder;
import java.util.function.Function;

public class CallTaskJavaBuilder extends TaskBaseBuilder<CallTaskJavaBuilder>
    implements JavaTransformationHandlers<CallTaskJavaBuilder> {

  private CallTaskJava callTaskJava;

  CallTaskJavaBuilder() {
    callTaskJava = new CallTaskJava(new CallJava() {});
    super.setTask(callTaskJava.getCallJava());
  }

  @Override
  protected CallTaskJavaBuilder self() {
    return this;
  }

  public <T, V> CallTaskJavaBuilder fn(Function<T, V> function) {
    this.callTaskJava = new CallTaskJava(CallJava.function(function));
    super.setTask(this.callTaskJava.getCallJava());
    return this;
  }

  public CallTaskJava build() {
    return this.callTaskJava;
  }
}
