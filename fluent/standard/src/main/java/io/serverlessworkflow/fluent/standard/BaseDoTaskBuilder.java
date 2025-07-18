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

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class BaseDoTaskBuilder<T extends BaseDoTaskBuilder<T>> extends TaskBaseBuilder<T> {
  private final DoTask doTask;
  private final List<TaskItem> list;

  protected BaseDoTaskBuilder() {
    this.doTask = new DoTask();
    this.list = new ArrayList<>();
    this.setTask(doTask);
  }

  protected abstract T self();

  protected abstract T newDo();

  protected final void addTaskItem(TaskItem taskItem) {
    if (taskItem != null) {
      this.list.add(taskItem);
    }
  }

  protected final void addTaskItem(List<TaskItem> taskItem) {
    if (taskItem != null) {
      this.list.addAll(taskItem);
    }
  }

  public T set(String name, Consumer<SetTaskBuilder> itemsConfigurer) {
    final SetTaskBuilder setBuilder = new SetTaskBuilder();
    itemsConfigurer.accept(setBuilder);
    this.list.add(new TaskItem(name, new Task().withSetTask(setBuilder.build())));
    return self();
  }

  public T set(Consumer<SetTaskBuilder> itemsConfigurer) {
    return this.set(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T set(String name, final String expr) {
    return this.set(name, s -> s.expr(expr));
  }

  public T set(final String expr) {
    return this.set(UUID.randomUUID().toString(), s -> s.expr(expr));
  }

  public T forEach(String name, ForEach<T> itemsConfigurer) {
    final ForTaskBuilder<T> forBuilder = new ForTaskBuilder<>(newDo());
    itemsConfigurer.configure(forBuilder);
    this.list.add(new TaskItem(name, new Task().withForTask(forBuilder.build())));
    return self();
  }

  public T forEach(ForEach<T> itemsConfigurer) {
    return this.forEach(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T switchCase(String name, SwitchCase itemsConfigurer) {
    final SwitchTaskBuilder switchBuilder = new SwitchTaskBuilder();
    itemsConfigurer.configure(switchBuilder);
    this.list.add(new TaskItem(name, new Task().withSwitchTask(switchBuilder.build())));
    return self();
  }

  public T switchCase(SwitchCase itemsConfigurer) {
    return this.switchCase(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer) {
    final RaiseTaskBuilder raiseBuilder = new RaiseTaskBuilder();
    itemsConfigurer.accept(raiseBuilder);
    this.list.add(new TaskItem(name, new Task().withRaiseTask(raiseBuilder.build())));
    return self();
  }

  public T raise(Consumer<RaiseTaskBuilder> itemsConfigurer) {
    return this.raise(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer) {
    final ForkTaskBuilder forkBuilder = new ForkTaskBuilder();
    itemsConfigurer.accept(forkBuilder);
    this.list.add(new TaskItem(name, new Task().withForkTask(forkBuilder.build())));
    return self();
  }

  public T fork(Consumer<ForkTaskBuilder> itemsConfigurer) {
    return this.fork(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer) {
    final ListenTaskBuilder listenBuilder = new ListenTaskBuilder();
    itemsConfigurer.accept(listenBuilder);
    this.list.add(new TaskItem(name, new Task().withListenTask(listenBuilder.build())));
    return self();
  }

  public T listen(Consumer<ListenTaskBuilder> itemsConfigurer) {
    return this.listen(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer) {
    final EmitTaskBuilder emitBuilder = new EmitTaskBuilder();
    itemsConfigurer.accept(emitBuilder);
    this.list.add(new TaskItem(name, new Task().withEmitTask(emitBuilder.build())));
    return self();
  }

  public T emit(Consumer<EmitTaskBuilder> itemsConfigurer) {
    return this.emit(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T tryTask(String name, Consumer<TryTaskBuilder<T>> itemsConfigurer) {
    final TryTaskBuilder<T> tryBuilder = new TryTaskBuilder<>(this.newDo());
    itemsConfigurer.accept(tryBuilder);
    this.list.add(new TaskItem(name, new Task().withTryTask(tryBuilder.build())));
    return self();
  }

  public T tryTask(Consumer<TryTaskBuilder<T>> itemsConfigurer) {
    return this.tryTask(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public T callHTTP(String name, Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    final CallHTTPTaskBuilder callHTTPBuilder = new CallHTTPTaskBuilder();
    itemsConfigurer.accept(callHTTPBuilder);
    this.list.add(
        new TaskItem(
            name, new Task().withCallTask(new CallTask().withCallHTTP(callHTTPBuilder.build()))));
    return self();
  }

  public T callHTTP(Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    return this.callHTTP(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public DoTask build() {
    this.doTask.setDo(this.list);
    return this.doTask;
  }
}
