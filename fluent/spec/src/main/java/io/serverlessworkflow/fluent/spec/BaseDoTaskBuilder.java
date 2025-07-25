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

import io.serverlessworkflow.api.types.DoTask;
import java.util.function.Consumer;

public abstract class BaseDoTaskBuilder<
        TASK extends TaskBaseBuilder<TASK>, LIST extends BaseTaskItemListBuilder<LIST>>
    extends TaskBaseBuilder<TASK> {
  private final DoTask doTask;
  private final BaseTaskItemListBuilder<LIST> taskItemListBuilder;

  protected BaseDoTaskBuilder(BaseTaskItemListBuilder<LIST> taskItemListBuilder) {
    this.doTask = new DoTask();
    this.taskItemListBuilder = taskItemListBuilder;
    this.setTask(doTask);
  }

  protected abstract TASK self();

  protected LIST innerListBuilder() {
    return (LIST) taskItemListBuilder;
  }

  public TASK set(String name, Consumer<SetTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.set(name, itemsConfigurer);
    return self();
  }

  public TASK set(Consumer<SetTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.set(itemsConfigurer);
    return self();
  }

  public TASK set(String name, final String expr) {
    taskItemListBuilder.set(name, s -> s.expr(expr));
    return self();
  }

  public TASK set(final String expr) {
    taskItemListBuilder.set(expr);
    return self();
  }

  public TASK forEach(String name, Consumer<ForTaskBuilder<LIST>> itemsConfigurer) {
    taskItemListBuilder.forEach(name, itemsConfigurer);
    return self();
  }

  public TASK forEach(Consumer<ForTaskBuilder<LIST>> itemsConfigurer) {
    taskItemListBuilder.forEach(itemsConfigurer);
    return self();
  }

  public TASK switchC(String name, Consumer<SwitchTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.switchC(name, itemsConfigurer);
    return self();
  }

  public TASK switchC(Consumer<SwitchTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.switchC(itemsConfigurer);
    return self();
  }

  public TASK raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.raise(name, itemsConfigurer);
    return self();
  }

  public TASK raise(Consumer<RaiseTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.raise(itemsConfigurer);
    return self();
  }

  public TASK fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.fork(name, itemsConfigurer);
    return self();
  }

  public TASK fork(Consumer<ForkTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.fork(itemsConfigurer);
    return self();
  }

  public TASK listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.listen(name, itemsConfigurer);
    return self();
  }

  public TASK listen(Consumer<ListenTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.listen(itemsConfigurer);
    return self();
  }

  public TASK emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.emit(name, itemsConfigurer);
    return self();
  }

  public TASK emit(Consumer<EmitTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.emit(itemsConfigurer);
    return self();
  }

  public TASK tryC(String name, Consumer<TryTaskBuilder<LIST>> itemsConfigurer) {
    taskItemListBuilder.tryC(name, itemsConfigurer);
    return self();
  }

  public TASK tryC(Consumer<TryTaskBuilder<LIST>> itemsConfigurer) {
    taskItemListBuilder.tryC(itemsConfigurer);
    return self();
  }

  public TASK callHTTP(String name, Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.callHTTP(name, itemsConfigurer);
    return self();
  }

  public TASK callHTTP(Consumer<CallHTTPTaskBuilder> itemsConfigurer) {
    taskItemListBuilder.callHTTP(itemsConfigurer);
    return self();
  }

  public TASK callAgentAI(String name, Consumer<CallAgentAITaskBuilder> itemsConfigurer) {
    taskItemListBuilder.callAgentAI(name, itemsConfigurer);
    return self();
  }

  public TASK callAgentAI(Consumer<CallAgentAITaskBuilder> itemsConfigurer) {
    taskItemListBuilder.callAgentAI(itemsConfigurer);
    return self();
  }

  public DoTask build() {
    this.doTask.setDo(this.taskItemListBuilder.build());
    return this.doTask;
  }
}
