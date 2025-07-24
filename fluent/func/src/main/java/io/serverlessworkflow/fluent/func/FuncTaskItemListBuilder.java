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

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.spec.BaseTaskItemListBuilder;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class FuncTaskItemListBuilder extends BaseTaskItemListBuilder<FuncTaskItemListBuilder>
    implements FuncDoTaskFluent<FuncTaskItemListBuilder> {

  public FuncTaskItemListBuilder() {
    super();
  }

  public FuncTaskItemListBuilder(final List<TaskItem> list) {
    super(list);
  }

  @Override
  protected FuncTaskItemListBuilder self() {
    return this;
  }

  @Override
  protected FuncTaskItemListBuilder newItemListBuilder() {
    return new FuncTaskItemListBuilder();
  }

  @Override
  public FuncTaskItemListBuilder callFn(String name, Consumer<FuncCallTaskBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final FuncCallTaskBuilder callTaskJavaBuilder = new FuncCallTaskBuilder();
    consumer.accept(callTaskJavaBuilder);
    return addTaskItem(new TaskItem(name, new Task().withCallTask(callTaskJavaBuilder.build())));
  }

  @Override
  public FuncTaskItemListBuilder callFn(Consumer<FuncCallTaskBuilder> consumer) {
    return this.callFn(UUID.randomUUID().toString(), consumer);
  }

  @Override
  public FuncTaskItemListBuilder forFn(String name, Consumer<FuncForTaskBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final FuncForTaskBuilder forTaskJavaBuilder = new FuncForTaskBuilder();
    consumer.accept(forTaskJavaBuilder);
    return this.addTaskItem(new TaskItem(name, new Task().withForTask(forTaskJavaBuilder.build())));
  }

  @Override
  public FuncTaskItemListBuilder forFn(Consumer<FuncForTaskBuilder> consumer) {
    return this.forFn(UUID.randomUUID().toString(), consumer);
  }

  @Override
  public FuncTaskItemListBuilder switchFn(String name, Consumer<FuncSwitchTaskBuilder> consumer) {
    this.requireNameAndConfig(name, consumer);
    final FuncSwitchTaskBuilder funcSwitchTaskBuilder = new FuncSwitchTaskBuilder();
    consumer.accept(funcSwitchTaskBuilder);
    return this.addTaskItem(
        new TaskItem(name, new Task().withSwitchTask(funcSwitchTaskBuilder.build())));
  }

  @Override
  public FuncTaskItemListBuilder switchFn(Consumer<FuncSwitchTaskBuilder> consumer) {
    return this.switchFn(UUID.randomUUID().toString(), consumer);
  }

  @Override
  public FuncTaskItemListBuilder forkFn(Consumer<FuncForkTaskBuilder> cfg) {
    return this.forkFn(UUID.randomUUID().toString(), cfg);
  }

  @Override
  public FuncTaskItemListBuilder forkFn(String name, Consumer<FuncForkTaskBuilder> cfg) {
    this.requireNameAndConfig(name, cfg);
    final FuncForkTaskBuilder forkTaskJavaBuilder = new FuncForkTaskBuilder();
    cfg.accept(forkTaskJavaBuilder);
    return this.addTaskItem(
        new TaskItem(name, new Task().withForkTask(forkTaskJavaBuilder.build())));
  }
}
