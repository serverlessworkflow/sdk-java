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

import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SwitchTaskBuilder extends TaskBaseBuilder<SwitchTaskBuilder> {

  private final SwitchTask switchTask;
  private final List<SwitchItem> switchItems;

  SwitchTaskBuilder() {
    super();
    this.switchTask = new SwitchTask();
    this.switchItems = new ArrayList<>();
    this.setTask(switchTask);
  }

  @Override
  protected SwitchTaskBuilder self() {
    return this;
  }

  public SwitchTaskBuilder items(Consumer<SwitchCaseBuilder> switchCaseConsumer) {
    return this.items(UUID.randomUUID().toString(), switchCaseConsumer);
  }

  public SwitchTaskBuilder items(
      final String name, Consumer<SwitchCaseBuilder> switchCaseConsumer) {
    final SwitchCaseBuilder switchCaseBuilder = new SwitchCaseBuilder();
    switchCaseConsumer.accept(switchCaseBuilder);
    this.switchItems.add(new SwitchItem(name, switchCaseBuilder.build()));
    return this;
  }

  public SwitchTask build() {
    this.switchTask.setSwitch(this.switchItems);
    return this.switchTask;
  }

  public static final class SwitchCaseBuilder {
    private final SwitchCase switchCase;

    SwitchCaseBuilder() {
      this.switchCase = new SwitchCase();
    }

    public SwitchCaseBuilder when(String when) {
      this.switchCase.setWhen(when);
      return this;
    }

    public SwitchCaseBuilder then(FlowDirective then) {
      this.switchCase.setThen(then);
      return this;
    }

    public SwitchCaseBuilder then(FlowDirectiveEnum then) {
      this.switchCase.setThen(new FlowDirective().withFlowDirectiveEnum(then));
      return this;
    }

    public SwitchCase build() {
      return this.switchCase;
    }
  }
}
