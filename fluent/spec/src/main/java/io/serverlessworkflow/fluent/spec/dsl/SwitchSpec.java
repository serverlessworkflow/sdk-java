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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.fluent.spec.SwitchTaskBuilder;
import io.serverlessworkflow.fluent.spec.configurers.SwitchConfigurer;
import io.serverlessworkflow.fluent.spec.spi.SwitchTaskFluent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class SwitchSpec implements SwitchConfigurer {

  private final Map<String, Consumer<SwitchTaskFluent.SwitchCaseBuilder>> cases =
      new LinkedHashMap<>();
  private boolean defaultSet = false;

  public SwitchSpec on(String caseKey, String whenExpr, String thenKey) {
    Objects.requireNonNull(caseKey);
    Objects.requireNonNull(whenExpr);
    Objects.requireNonNull(thenKey);
    putCase(caseKey, c -> c.when(whenExpr).then(thenKey));
    return this;
  }

  public SwitchSpec on(String caseKey, String whenExpr, FlowDirectiveEnum thenKey) {
    Objects.requireNonNull(caseKey);
    Objects.requireNonNull(whenExpr);
    Objects.requireNonNull(thenKey);
    putCase(caseKey, c -> c.when(whenExpr).then(thenKey));
    return this;
  }

  public SwitchSpec onDefault(String thenKey) {
    Objects.requireNonNull(thenKey);
    setDefault(c -> c.then(thenKey));
    return this;
  }

  public SwitchSpec onDefault(FlowDirectiveEnum thenKey) {
    Objects.requireNonNull(thenKey);
    setDefault(c -> c.then(thenKey));
    return this;
  }

  private void putCase(String key, Consumer<SwitchTaskFluent.SwitchCaseBuilder> cfg) {
    if (SwitchTaskFluent.DEFAULT_CASE.equals(key)) {
      throw new IllegalArgumentException("Use onDefault(...) for the default case.");
    }
    if (cases.putIfAbsent(key, cfg) != null) {
      throw new IllegalStateException("Duplicate switch case key: " + key);
    }
  }

  private void setDefault(Consumer<SwitchTaskFluent.SwitchCaseBuilder> cfg) {
    if (defaultSet) {
      throw new IllegalStateException("Default case already defined.");
    }
    defaultSet = true;
    cases.put(SwitchTaskFluent.DEFAULT_CASE, cfg);
  }

  @Override
  public void accept(SwitchTaskBuilder switchTaskBuilder) {
    cases.forEach(switchTaskBuilder::on);
  }
}
