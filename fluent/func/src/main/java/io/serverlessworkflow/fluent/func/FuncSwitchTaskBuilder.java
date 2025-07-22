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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchItem;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.func.SwitchCaseFunction;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FuncSwitchTaskBuilder extends TaskBaseBuilder<FuncSwitchTaskBuilder>
    implements FuncTransformations<FuncSwitchTaskBuilder> {

  private final SwitchTask switchTask;
  private final List<SwitchItem> switchItems;

  FuncSwitchTaskBuilder() {
    this.switchTask = new SwitchTask();
    this.switchItems = new ArrayList<>();
    super.setTask(switchTask);
  }

  @Override
  protected FuncSwitchTaskBuilder self() {
    return this;
  }

  public FuncSwitchTaskBuilder items(Consumer<SwitchCaseJavaBuilder> consumer) {
    return this.items(UUID.randomUUID().toString(), consumer);
  }

  public FuncSwitchTaskBuilder items(String name, Consumer<SwitchCaseJavaBuilder> consumer) {
    final SwitchCaseJavaBuilder switchCase = new SwitchCaseJavaBuilder();
    consumer.accept(switchCase);
    this.switchItems.add(new SwitchItem(name, switchCase.build()));
    return this;
  }

  public SwitchTask build() {
    this.switchTask.setSwitch(this.switchItems);
    return switchTask;
  }

  public static final class SwitchCaseJavaBuilder {
    private final SwitchCaseFunction switchCase;

    SwitchCaseJavaBuilder() {
      this.switchCase = new SwitchCaseFunction();
    }

    public <T> SwitchCaseJavaBuilder when(Predicate<T> when) {
      this.switchCase.setPredicate(when);
      return this;
    }

    public SwitchCaseJavaBuilder then(FlowDirective then) {
      this.switchCase.setThen(then);
      return this;
    }

    public SwitchCaseJavaBuilder then(FlowDirectiveEnum then) {
      this.switchCase.setThen(new FlowDirective().withFlowDirectiveEnum(then));
      return this;
    }

    public SwitchCase build() {
      return this.switchCase;
    }
  }
}
