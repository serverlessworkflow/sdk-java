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

import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.fluent.spec.spi.ForEachTaskFluent;
import java.util.function.Consumer;

public class ForEachTaskBuilder<T extends BaseTaskItemListBuilder<T>>
    extends TaskBaseBuilder<ForEachTaskBuilder<T>>
    implements ForEachTaskFluent<ForEachTaskBuilder<T>, T> {

  private final ForTask forTask;
  private final ForTaskConfiguration forTaskConfiguration;
  private final T taskItemListBuilder;

  public ForEachTaskBuilder(T taskItemListBuilder) {
    super();
    forTask = new ForTask();
    forTaskConfiguration = new ForTaskConfiguration();
    this.taskItemListBuilder = taskItemListBuilder;
    super.setTask(forTask);
  }

  protected ForEachTaskBuilder<T> self() {
    return this;
  }

  public ForEachTaskBuilder<T> each(String each) {
    forTaskConfiguration.setEach(each);
    return this;
  }

  public ForEachTaskBuilder<T> in(String in) {
    this.forTaskConfiguration.setIn(in);
    return this;
  }

  public ForEachTaskBuilder<T> at(String at) {
    this.forTaskConfiguration.setAt(at);
    return this;
  }

  public ForEachTaskBuilder<T> whileC(final String expression) {
    this.forTask.setWhile(expression);
    return this;
  }

  public ForEachTaskBuilder<T> tasks(Consumer<T> doBuilderConsumer) {
    final T taskItemListBuilder = this.taskItemListBuilder.newItemListBuilder();
    doBuilderConsumer.accept(taskItemListBuilder);
    this.forTask.setDo(taskItemListBuilder.build());
    return this;
  }

  public ForTask build() {
    this.forTask.setFor(this.forTaskConfiguration);
    return this.forTask;
  }
}
