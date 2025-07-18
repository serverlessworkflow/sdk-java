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

import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import java.util.function.Consumer;

public class ForTaskBuilder extends TaskBaseBuilder<ForTaskBuilder> {

  private final ForTask forTask;
  private final ForTaskConfiguration forTaskConfiguration;

  ForTaskBuilder() {
    super();
    forTask = new ForTask();
    forTaskConfiguration = new ForTaskConfiguration();
    super.setTask(forTask);
  }

  protected ForTaskBuilder self() {
    return this;
  }

  public ForTaskBuilder each(String each) {
    forTaskConfiguration.setEach(each);
    return this;
  }

  public ForTaskBuilder in(String in) {
    this.forTaskConfiguration.setIn(in);
    return this;
  }

  public ForTaskBuilder at(String at) {
    this.forTaskConfiguration.setAt(at);
    return this;
  }

  public ForTaskBuilder whileCondition(final String expression) {
    this.forTask.setWhile(expression);
    return this;
  }

  public ForTaskBuilder doTasks(Consumer<DoTaskBuilder> doBuilderConsumer) {
    final DoTaskBuilder doTaskBuilder = new DoTaskBuilder();
    doBuilderConsumer.accept(doTaskBuilder);
    this.forTask.setDo(doTaskBuilder.build().getDo());
    return this;
  }

  public ForTask build() {
    this.forTask.setFor(this.forTaskConfiguration);
    return this.forTask;
  }
}
