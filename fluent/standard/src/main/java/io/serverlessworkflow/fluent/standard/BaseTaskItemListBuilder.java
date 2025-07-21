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
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A builder for an ordered {@link TaskItem} list.
 *
 * <p>This builder only knows how to append new TaskItems of various flavors, but does NOT expose
 * {@link TaskBase}‑level methods like export(), input(), etc. Those belong on {@link
 * TaskBaseBuilder} subclasses.
 *
 * @param <SELF> the concrete builder type
 */
public abstract class BaseTaskItemListBuilder<SELF extends BaseTaskItemListBuilder<SELF>> {

  private final List<TaskItem> list;

  public BaseTaskItemListBuilder() {
    this.list = new ArrayList<>();
  }

  protected abstract SELF self();

  protected abstract SELF newItemListBuilder();

  protected SELF addTaskItem(TaskItem taskItem) {
    Objects.requireNonNull(taskItem, "taskItem must not be null");
    list.add(taskItem);
    return self();
  }

  protected void requireNameAndConfig(String name, Consumer<?> cfg) {
    Objects.requireNonNull(name, "Task name must not be null");
    Objects.requireNonNull(cfg, "Configurer must not be null");
  }

  public SELF set(String name, Consumer<SetTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final SetTaskBuilder setBuilder = new SetTaskBuilder();
    itemsConfigurer.accept(setBuilder);
    return addTaskItem(new TaskItem(name, new Task().withSetTask(setBuilder.build())));
  }

  public SELF set(Consumer<SetTaskBuilder> itemsConfigurer) {
    return this.set(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF set(String name, final String expr) {
    return this.set(name, s -> s.expr(expr));
  }

  public SELF set(final String expr) {
    return this.set(UUID.randomUUID().toString(), s -> s.expr(expr));
  }

  public SELF forEach(String name, Consumer<ForTaskBuilder<SELF>> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final ForTaskBuilder<SELF> forBuilder = new ForTaskBuilder<>(newItemListBuilder());
    itemsConfigurer.accept(forBuilder);
    return addTaskItem(new TaskItem(name, new Task().withForTask(forBuilder.build())));
  }

  public SELF forEach(Consumer<ForTaskBuilder<SELF>> itemsConfigurer) {
    return this.forEach(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF switchC(String name, Consumer<SwitchTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final SwitchTaskBuilder switchBuilder = new SwitchTaskBuilder();
    itemsConfigurer.accept(switchBuilder);
    return addTaskItem(new TaskItem(name, new Task().withSwitchTask(switchBuilder.build())));
  }

  public SELF switchC(Consumer<SwitchTaskBuilder> itemsConfigurer) {
    return this.switchC(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final RaiseTaskBuilder raiseBuilder = new RaiseTaskBuilder();
    itemsConfigurer.accept(raiseBuilder);
    return addTaskItem(new TaskItem(name, new Task().withRaiseTask(raiseBuilder.build())));
  }

  public SELF raise(Consumer<RaiseTaskBuilder> itemsConfigurer) {
    return this.raise(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final ForkTaskBuilder forkBuilder = new ForkTaskBuilder();
    itemsConfigurer.accept(forkBuilder);
    return addTaskItem(new TaskItem(name, new Task().withForkTask(forkBuilder.build())));
  }

  public SELF fork(Consumer<ForkTaskBuilder> itemsConfigurer) {
    return this.fork(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final ListenTaskBuilder listenBuilder = new ListenTaskBuilder();
    itemsConfigurer.accept(listenBuilder);
    return addTaskItem(new TaskItem(name, new Task().withListenTask(listenBuilder.build())));
  }

  public SELF listen(Consumer<ListenTaskBuilder> itemsConfigurer) {
    return this.listen(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final EmitTaskBuilder emitBuilder = new EmitTaskBuilder();
    itemsConfigurer.accept(emitBuilder);
    return addTaskItem(new TaskItem(name, new Task().withEmitTask(emitBuilder.build())));
  }

  public SELF emit(Consumer<EmitTaskBuilder> itemsConfigurer) {
    return this.emit(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF tryC(String name, Consumer<TryTaskBuilder<SELF>> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final TryTaskBuilder<SELF> tryBuilder = new TryTaskBuilder<>(this.newItemListBuilder());
    itemsConfigurer.accept(tryBuilder);
    return addTaskItem(new TaskItem(name, new Task().withTryTask(tryBuilder.build())));
  }

  public SELF tryC(Consumer<TryTaskBuilder<SELF>> itemsConfigurer) {
    return this.tryC(UUID.randomUUID().toString(), itemsConfigurer);
  }

  public SELF callHTTP(String name, Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    requireNameAndConfig(name, itemsConfigurer);
    final CallHTTPTaskBuilder callHTTPBuilder = new CallHTTPTaskBuilder();
    itemsConfigurer.accept(callHTTPBuilder);
    return addTaskItem(
        new TaskItem(
            name, new Task().withCallTask(new CallTask().withCallHTTP(callHTTPBuilder.build()))));
  }

  public SELF callHTTP(Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    return this.callHTTP(UUID.randomUUID().toString(), itemsConfigurer);
  }

  /**
   * @return an immutable snapshot of all {@link TaskItem}s added so far
   */
  public List<TaskItem> build() {
    return Collections.unmodifiableList(list);
  }
}
