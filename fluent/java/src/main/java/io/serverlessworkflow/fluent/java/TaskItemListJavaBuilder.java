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
import io.serverlessworkflow.fluent.standard.BaseTaskItemListBuilder;
import java.util.UUID;
import java.util.function.Consumer;

public class TaskItemListJavaBuilder extends BaseTaskItemListBuilder<TaskItemListJavaBuilder> {

  TaskItemListJavaBuilder() {
    super();
  }

  @Override
  protected TaskItemListJavaBuilder self() {
    return this;
  }

  @Override
  protected TaskItemListJavaBuilder newItemListBuilder() {
    return new TaskItemListJavaBuilder();
  }

  public TaskItemListJavaBuilder callFn(String name, Consumer<CallTaskJavaBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final CallTaskJavaBuilder callTaskJavaBuilder = new CallTaskJavaBuilder();
    consumer.accept(callTaskJavaBuilder);
    return addTaskItem(new TaskItem(name, new Task().withCallTask(callTaskJavaBuilder.build())));
  }

  public TaskItemListJavaBuilder callFn(Consumer<CallTaskJavaBuilder> consumer) {
    return this.callFn(UUID.randomUUID().toString(), consumer);
  }

  public TaskItemListJavaBuilder forEachFn(String name, Consumer<ForTaskJavaBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final ForTaskJavaBuilder forTaskJavaBuilder = new ForTaskJavaBuilder();
    consumer.accept(forTaskJavaBuilder);
    return this.addTaskItem(new TaskItem(name, new Task().withForTask(forTaskJavaBuilder.build())));
  }

  public TaskItemListJavaBuilder forEachFn(Consumer<ForTaskJavaBuilder> consumer) {
    return this.forEachFn(UUID.randomUUID().toString(), consumer);
  }

  public TaskItemListJavaBuilder switchCaseFn(
      String name, Consumer<SwitchTaskJavaBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final SwitchTaskJavaBuilder switchTaskJavaBuilder = new SwitchTaskJavaBuilder();
    consumer.accept(switchTaskJavaBuilder);
    return this.addTaskItem(
        new TaskItem(name, new Task().withSwitchTask(switchTaskJavaBuilder.build())));
  }

  public TaskItemListJavaBuilder switchCaseFn(Consumer<SwitchTaskJavaBuilder> consumer) {
    return this.switchCaseFn(UUID.randomUUID().toString(), consumer);
  }
}
