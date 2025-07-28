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
package io.serverlessworkflow.fluent.spec.spi;

import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.fluent.spec.TaskBaseBuilder;
import java.util.UUID;
import java.util.function.Consumer;

public interface SwitchTaskFluent<SELF extends TaskBaseBuilder<SELF>> {
  default SELF items(Consumer<SwitchTaskFluent.SwitchCaseBuilder> switchCaseConsumer) {
    return this.items(UUID.randomUUID().toString(), switchCaseConsumer);
  }

  SELF items(final String name, Consumer<SwitchTaskFluent.SwitchCaseBuilder> switchCaseConsumer);

  SwitchTask build();

  final class SwitchCaseBuilder {
    private final SwitchCase switchCase;

    public SwitchCaseBuilder() {
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
