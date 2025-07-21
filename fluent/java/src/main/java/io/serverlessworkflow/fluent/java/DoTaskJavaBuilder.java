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

import io.serverlessworkflow.fluent.standard.BaseDoTaskBuilder;
import java.util.function.Consumer;

public class DoTaskJavaBuilder extends BaseDoTaskBuilder<DoTaskJavaBuilder, TaskItemListJavaBuilder>
    implements JavaTransformationHandlers<DoTaskJavaBuilder> {

  DoTaskJavaBuilder() {
    super(new TaskItemListJavaBuilder());
  }

  @Override
  protected DoTaskJavaBuilder self() {
    return this;
  }

  public DoTaskJavaBuilder callFn(String name, Consumer<CallTaskJavaBuilder> consumer) {
    this.innerListBuilder().callJava(name, consumer);
    return this;
  }

  public DoTaskJavaBuilder callFn(Consumer<CallTaskJavaBuilder> consumer) {
    this.innerListBuilder().callJava(consumer);
    return this;
  }

  public DoTaskJavaBuilder forFn(String name, Consumer<ForTaskJavaBuilder> consumer) {
    this.innerListBuilder().forFn(name, consumer);
    return this;
  }

  public DoTaskJavaBuilder forFn(Consumer<ForTaskJavaBuilder> consumer) {
    this.innerListBuilder().forFn(consumer);
    return this;
  }

  public DoTaskJavaBuilder switchFn(String name, Consumer<SwitchTaskJavaBuilder> consumer) {
    this.innerListBuilder().switchFn(name, consumer);
    return this;
  }

  public DoTaskJavaBuilder switchFn(Consumer<SwitchTaskJavaBuilder> consumer) {
    this.innerListBuilder().switchFn(consumer);
    return this;
  }
}
