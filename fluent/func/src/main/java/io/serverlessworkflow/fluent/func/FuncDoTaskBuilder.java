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
import java.util.function.Consumer;

public class FuncDoTaskBuilder extends BaseDoTaskBuilder<FuncDoTaskBuilder, FuncTaskItemListBuilder>
    implements FuncTransformations<FuncDoTaskBuilder> {

  FuncDoTaskBuilder() {
    super(new FuncTaskItemListBuilder());
  }

  @Override
  protected FuncDoTaskBuilder self() {
    return this;
  }

  public FuncDoTaskBuilder callFn(String name, Consumer<FuncCallTaskBuilder> consumer) {
    this.innerListBuilder().callJava(name, consumer);
    return this;
  }

  public FuncDoTaskBuilder callFn(Consumer<FuncCallTaskBuilder> consumer) {
    this.innerListBuilder().callJava(consumer);
    return this;
  }

  public FuncDoTaskBuilder forFn(String name, Consumer<FuncForTaskBuilder> consumer) {
    this.innerListBuilder().forFn(name, consumer);
    return this;
  }

  public FuncDoTaskBuilder forFn(Consumer<FuncForTaskBuilder> consumer) {
    this.innerListBuilder().forFn(consumer);
    return this;
  }

  public FuncDoTaskBuilder switchFn(String name, Consumer<FuncSwitchTaskBuilder> consumer) {
    this.innerListBuilder().switchFn(name, consumer);
    return this;
  }

  public FuncDoTaskBuilder switchFn(Consumer<FuncSwitchTaskBuilder> consumer) {
    this.innerListBuilder().switchFn(consumer);
    return this;
  }
}
