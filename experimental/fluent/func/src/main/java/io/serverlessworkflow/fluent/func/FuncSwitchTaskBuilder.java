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
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.func.spi.FuncTaskTransformations;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import io.serverlessworkflow.fluent.spec.spi.SwitchTaskFluent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FuncSwitchTaskBuilder extends TaskBaseBuilder<FuncSwitchTaskBuilder>
    implements FuncTaskTransformations<FuncSwitchTaskBuilder>,
        ConditionalTaskBuilder<FuncSwitchTaskBuilder>,
        SwitchTaskFluent<FuncSwitchTaskBuilder> {

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

  public FuncSwitchTaskBuilder onPredicate(Consumer<SwitchCasePredicateBuilder> consumer) {
    return this.onPredicate(UUID.randomUUID().toString(), consumer);
  }

  public FuncSwitchTaskBuilder onPredicate(
      String name, Consumer<SwitchCasePredicateBuilder> consumer) {
    final SwitchCasePredicateBuilder switchCase = new SwitchCasePredicateBuilder();
    consumer.accept(switchCase);
    final SwitchCaseFunction switchCaseValue = (SwitchCaseFunction) switchCase.build();

    // Handling default cases
    if (switchCaseValue.predicate() == null) {
      Objects.requireNonNull(switchCaseValue.getThen(), "When is required");
      if (switchCaseValue.getThen().getFlowDirectiveEnum() != null) {
        return this.onDefault(switchCaseValue.getThen().getFlowDirectiveEnum());
      } else {
        return this.onDefault(switchCaseValue.getThen().getString());
      }
    }

    this.switchItems.add(new SwitchItem(name, switchCase.build()));
    return this;
  }

  @Override
  public FuncSwitchTaskBuilder on(String name, Consumer<SwitchCaseBuilder> switchCaseConsumer) {
    final SwitchCaseBuilder switchCase = new SwitchCaseBuilder();
    switchCaseConsumer.accept(switchCase);
    this.switchItems.add(new SwitchItem(name, switchCase.build()));
    return this;
  }

  public SwitchTask build() {
    this.switchTask.setSwitch(this.switchItems);
    return switchTask;
  }

  public static final class SwitchCasePredicateBuilder {
    private final SwitchCaseFunction switchCase;

    SwitchCasePredicateBuilder() {
      this.switchCase = new SwitchCaseFunction();
    }

    public <T> SwitchCasePredicateBuilder when(Predicate<T> when) {
      this.switchCase.withPredicate(when);
      return this;
    }

    public <T> SwitchCasePredicateBuilder when(Predicate<T> when, Class<T> whenClass) {
      this.switchCase.withPredicate(when, whenClass);
      return this;
    }

    public SwitchCasePredicateBuilder then(String taskName) {
      this.switchCase.setThen(new FlowDirective().withString(taskName));
      return this;
    }

    public SwitchCasePredicateBuilder then(FlowDirectiveEnum then) {
      this.switchCase.setThen(new FlowDirective().withFlowDirectiveEnum(then));
      return this;
    }

    public SwitchCase build() {
      return this.switchCase;
    }
  }
}
