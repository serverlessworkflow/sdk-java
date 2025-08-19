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

import io.serverlessworkflow.api.types.CorrelateProperty;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.ListenTaskConfiguration;
import io.serverlessworkflow.api.types.ListenTo;
import java.util.function.Consumer;

/**
 * Fluent builder for a "listen" task in a Serverless Workflow. Enforces exactly one consumption
 * strategy: one, all, or any.
 */
public class ListenTaskBuilder<T extends BaseTaskItemListBuilder<T>>
    extends TaskBaseBuilder<ListenTaskBuilder<T>> {

  private final ListenTask listenTask;
  private final ListenTaskConfiguration config;
  private final T taskItemListBuilder;

  public ListenTaskBuilder(T taskItemListBuilder) {
    super();
    this.listenTask = new ListenTask();
    this.config = new ListenTaskConfiguration();
    this.config.setTo(new ListenTo());
    this.listenTask.setListen(config);
    this.taskItemListBuilder = taskItemListBuilder;
    super.setTask(listenTask);
  }

  @Override
  protected ListenTaskBuilder<T> self() {
    return this;
  }

  public ListenTaskBuilder<T> forEach(Consumer<SubscriptionIteratorBuilder<T>> c) {
    final SubscriptionIteratorBuilder<T> iteratorBuilder =
        new SubscriptionIteratorBuilder<>(this.taskItemListBuilder);
    c.accept(iteratorBuilder);
    this.listenTask.setForeach(iteratorBuilder.build());
    return this;
  }

  public ListenTaskBuilder<T> read(ListenTaskConfiguration.ListenAndReadAs listenAndReadAs) {
    this.config.setRead(listenAndReadAs);
    return this;
  }

  public ListenTaskBuilder<T> to(Consumer<ListenToBuilder> c) {
    final ListenToBuilder listenToBuilder = new ListenToBuilder();
    c.accept(listenToBuilder);
    this.config.setTo(listenToBuilder.build());
    return this;
  }

  public ListenTask build() {
    return listenTask;
  }

  public static final class CorrelatePropertyBuilder {
    private final CorrelateProperty prop = new CorrelateProperty();

    public CorrelatePropertyBuilder from(String expr) {
      prop.setFrom(expr);
      return this;
    }

    public CorrelatePropertyBuilder expect(String val) {
      prop.setExpect(val);
      return this;
    }

    public CorrelateProperty build() {
      return prop;
    }
  }
}
