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

import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.fluent.spec.spi.ForkTaskFluent;
import java.util.function.Consumer;

public class ForkTaskBuilder extends TaskBaseBuilder<ForkTaskBuilder>
    implements ForkTaskFluent<ForkTaskBuilder, TaskItemListBuilder> {

  private final ForkTask forkTask;
  private final ForkTaskConfiguration forkTaskConfiguration;

  ForkTaskBuilder() {
    this.forkTask = new ForkTask();
    this.forkTaskConfiguration = new ForkTaskConfiguration();
    super.setTask(this.forkTask);
  }

  @Override
  protected ForkTaskBuilder self() {
    return this;
  }

  @Override
  public ForkTaskBuilder compete(final boolean compete) {
    this.forkTaskConfiguration.setCompete(compete);
    return this;
  }

  @Override
  public ForkTaskBuilder branches(Consumer<TaskItemListBuilder> branchesConsumer) {
    final TaskItemListBuilder doTaskBuilder = new TaskItemListBuilder();
    branchesConsumer.accept(doTaskBuilder);
    this.forkTaskConfiguration.setBranches(doTaskBuilder.build());
    return this;
  }

  @Override
  public ForkTask build() {
    return this.forkTask.withFork(this.forkTaskConfiguration);
  }
}
