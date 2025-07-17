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
package io.serverlessworkflow.fluent.standard;

import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DoTaskBuilder extends TaskBaseBuilder<DoTaskBuilder> {

  private final DoTask doTask;
  private final List<TaskItem> list;

  DoTaskBuilder() {
    this.doTask = new DoTask();
    this.list = new ArrayList<>();
    this.setTask(doTask);
  }

  @Override
  protected DoTaskBuilder self() {
    return this;
  }

  public DoTaskBuilder set(String name, Consumer<SetTaskBuilder> itemsConfigurer) {
    final SetTaskBuilder setBuilder = new SetTaskBuilder();
    itemsConfigurer.accept(setBuilder);
    this.list.add(new TaskItem(name, new Task().withSetTask(setBuilder.build())));
    return this;
  }

  public DoTaskBuilder set(Consumer<SetTaskBuilder> itemsConfigurer) {
    return this.set(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder set(String name, final String expr) {
    return this.set(name, s -> s.expr(expr));
  }

  public DoTaskBuilder set(final String expr) {
    return this.set(UUID.randomUUID().toString(), s -> s.expr(expr));
  }

  public DoTaskBuilder forEach(String name, Consumer<ForTaskBuilder> itemsConfigurer) {
    final ForTaskBuilder forBuilder = new ForTaskBuilder();
    itemsConfigurer.accept(forBuilder);
    this.list.add(new TaskItem(name, new Task().withForTask(forBuilder.build())));
    return this;
  }

  public DoTaskBuilder forEach(Consumer<ForTaskBuilder> itemsConfigurer) {
    return this.forEach(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder switchTask(String name, Consumer<SwitchTaskBuilder> itemsConfigurer) {
    final SwitchTaskBuilder switchBuilder = new SwitchTaskBuilder();
    itemsConfigurer.accept(switchBuilder);
    this.list.add(new TaskItem(name, new Task().withSwitchTask(switchBuilder.build())));
    return this;
  }

  public DoTaskBuilder switchTask(Consumer<SwitchTaskBuilder> itemsConfigurer) {
    return this.switchTask(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer) {
    final RaiseTaskBuilder raiseBuilder = new RaiseTaskBuilder();
    itemsConfigurer.accept(raiseBuilder);
    this.list.add(new TaskItem(name, new Task().withRaiseTask(raiseBuilder.build())));
    return this;
  }

  public DoTaskBuilder raise(Consumer<RaiseTaskBuilder> itemsConfigurer) {
    return this.raise(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer) {
    final ForkTaskBuilder forkBuilder = new ForkTaskBuilder();
    itemsConfigurer.accept(forkBuilder);
    this.list.add(new TaskItem(name, new Task().withForkTask(forkBuilder.build())));
    return this;
  }

  public DoTaskBuilder fork(Consumer<ForkTaskBuilder> itemsConfigurer) {
    return this.fork(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer) {
    final ListenTaskBuilder listenBuilder = new ListenTaskBuilder();
    itemsConfigurer.accept(listenBuilder);
    this.list.add(new TaskItem(name, new Task().withListenTask(listenBuilder.build())));
    return this;
  }

  public DoTaskBuilder listen(Consumer<ListenTaskBuilder> itemsConfigurer) {
    return this.listen(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer) {
    final EmitTaskBuilder emitBuilder = new EmitTaskBuilder();
    itemsConfigurer.accept(emitBuilder);
    this.list.add(new TaskItem(name, new Task().withEmitTask(emitBuilder.build())));
    return this;
  }

  public DoTaskBuilder emit(Consumer<EmitTaskBuilder> itemsConfigurer) {
    return this.emit(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTaskBuilder tryTask(String name, Consumer<TryTaskBuilder> itemsConfigurer) {
    final TryTaskBuilder tryBuilder = new TryTaskBuilder();
    itemsConfigurer.accept(tryBuilder);
    this.list.add(new TaskItem(name, new Task().withTryTask(tryBuilder.build())));
    return this;
  }

  public DoTaskBuilder tryTask(Consumer<TryTaskBuilder> itemsConfigurer) {
    return this.tryTask(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTask build() {
    this.doTask.setDo(this.list);
    return this.doTask;
  }
}
