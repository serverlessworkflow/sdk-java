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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.spec.spi.DoFluent;
import java.util.List;
import java.util.function.Consumer;

public class TaskItemListBuilder extends BaseTaskItemListBuilder<TaskItemListBuilder>
    implements DoFluent<TaskItemListBuilder> {

  public TaskItemListBuilder() {
    super();
  }

  public TaskItemListBuilder(List<TaskItem> list) {
    super(list);
  }

  @Override
  protected TaskItemListBuilder self() {
    return this;
  }

  @Override
  protected TaskItemListBuilder newItemListBuilder() {
    return new TaskItemListBuilder();
  }

  @Override
  public TaskItemListBuilder set(String name, Consumer<SetTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final SetTaskBuilder setBuilder = new SetTaskBuilder();
    itemsConfigurer.accept(setBuilder);
    return addTaskItem(new TaskItem(name, new Task().withSetTask(setBuilder.build())));
  }

  @Override
  public TaskItemListBuilder set(String name, final String expr) {
    return this.set(name, s -> s.expr(expr));
  }

  @Override
  public TaskItemListBuilder forEach(
      String name, Consumer<ForEachTaskBuilder<TaskItemListBuilder>> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final ForEachTaskBuilder<TaskItemListBuilder> forBuilder =
        new ForEachTaskBuilder<>(newItemListBuilder());
    itemsConfigurer.accept(forBuilder);
    return addTaskItem(new TaskItem(name, new Task().withForTask(forBuilder.build())));
  }

  @Override
  public TaskItemListBuilder switchCase(String name, Consumer<SwitchTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final SwitchTaskBuilder switchBuilder = new SwitchTaskBuilder();
    itemsConfigurer.accept(switchBuilder);
    return addTaskItem(new TaskItem(name, new Task().withSwitchTask(switchBuilder.build())));
  }

  @Override
  public TaskItemListBuilder raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final RaiseTaskBuilder raiseBuilder = new RaiseTaskBuilder();
    itemsConfigurer.accept(raiseBuilder);
    return addTaskItem(new TaskItem(name, new Task().withRaiseTask(raiseBuilder.build())));
  }

  @Override
  public TaskItemListBuilder fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final ForkTaskBuilder forkBuilder = new ForkTaskBuilder();
    itemsConfigurer.accept(forkBuilder);
    return addTaskItem(new TaskItem(name, new Task().withForkTask(forkBuilder.build())));
  }

  @Override
  public TaskItemListBuilder listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final ListenTaskBuilder listenBuilder = new ListenTaskBuilder();
    itemsConfigurer.accept(listenBuilder);
    return addTaskItem(new TaskItem(name, new Task().withListenTask(listenBuilder.build())));
  }

  @Override
  public TaskItemListBuilder emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final EmitTaskBuilder emitBuilder = new EmitTaskBuilder();
    itemsConfigurer.accept(emitBuilder);
    return addTaskItem(new TaskItem(name, new Task().withEmitTask(emitBuilder.build())));
  }

  @Override
  public TaskItemListBuilder tryCatch(
      String name, Consumer<TryTaskBuilder<TaskItemListBuilder>> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final TryTaskBuilder<TaskItemListBuilder> tryBuilder =
        new TryTaskBuilder<>(this.newItemListBuilder());
    itemsConfigurer.accept(tryBuilder);
    return addTaskItem(new TaskItem(name, new Task().withTryTask(tryBuilder.build())));
  }

  @Override
  public TaskItemListBuilder http(String name, Consumer<CallHttpTaskBuilder> itemsConfigurer) {
    name = defaultNameAndRequireConfig(name, itemsConfigurer);
    final CallHttpTaskBuilder callHTTPBuilder = new CallHttpTaskBuilder();
    itemsConfigurer.accept(callHTTPBuilder);
    final CallTask callTask = new CallTask();
    callTask.setCallHTTP(callHTTPBuilder.build());
    final Task task = new Task();
    task.setCallTask(callTask);

    return addTaskItem(new TaskItem(name, task));
  }
}
