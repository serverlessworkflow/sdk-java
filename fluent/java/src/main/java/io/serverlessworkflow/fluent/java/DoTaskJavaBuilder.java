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

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.standard.BaseDoTaskBuilder;
import java.util.UUID;
import java.util.function.Consumer;

public class DoTaskJavaBuilder extends BaseDoTaskBuilder<DoTaskJavaBuilder>
    implements JavaTransformationHandlers<DoTaskJavaBuilder> {

  DoTaskJavaBuilder() {
    super();
  }

  @Override
  protected DoTaskJavaBuilder self() {
    return this;
  }

  @Override
  protected DoTaskJavaBuilder newDo() {
    return new DoTaskJavaBuilder();
  }

  public DoTaskJavaBuilder callFn(String name, Consumer<CallTaskJavaBuilder> consumer) {
    final CallTaskJavaBuilder callTaskJavaBuilder = new CallTaskJavaBuilder();
    consumer.accept(callTaskJavaBuilder);
    this.addTaskItem(new TaskItem(name, new Task().withCallTask(callTaskJavaBuilder.build())));
    return this;
  }

  public DoTaskJavaBuilder callFn(Consumer<CallTaskJavaBuilder> consumer) {
    return this.callFn(UUID.randomUUID().toString(), consumer);
  }

  public DoTaskJavaBuilder forEachFn(String name, Consumer<ForTaskJavaBuilder> consumer) {
    final ForTaskJavaBuilder forTaskJavaBuilder = new ForTaskJavaBuilder();
    consumer.accept(forTaskJavaBuilder);
    this.addTaskItem(new TaskItem(name, new Task().withForTask(forTaskJavaBuilder.build())));
    return this;
  }

  public DoTaskJavaBuilder forEachFn(Consumer<ForTaskJavaBuilder> consumer) {
    return this.forEachFn(UUID.randomUUID().toString(), consumer);
  }

  public DoTaskJavaBuilder switchCaseFn(String name, Consumer<SwitchTaskJavaBuilder> consumer) {
    final SwitchTaskJavaBuilder switchTaskJavaBuilder = new SwitchTaskJavaBuilder();
    consumer.accept(switchTaskJavaBuilder);
    this.addTaskItem(new TaskItem(name, new Task().withSwitchTask(switchTaskJavaBuilder.build())));
    return this;
  }

  public DoTaskJavaBuilder switchCaseFn(Consumer<SwitchTaskJavaBuilder> consumer) {
    return this.switchCaseFn(UUID.randomUUID().toString(), consumer);
  }
}
