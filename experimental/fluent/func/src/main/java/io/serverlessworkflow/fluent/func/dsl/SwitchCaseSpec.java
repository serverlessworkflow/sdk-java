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
package io.serverlessworkflow.fluent.func.dsl;

import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import java.util.function.Predicate;

public class SwitchCaseSpec<T> implements SwitchCaseConfigurer {

  private String then = "";
  private Predicate<T> when;
  private Class<T> whenClass;

  public SwitchCaseSpec<T> when(Predicate<T> when, Class<T> whenClass) {
    this.when = when;
    this.whenClass = whenClass;
    return this;
  }

  public SwitchCaseSpec<T> when(Predicate<T> when) {
    this.when = when;
    return this;
  }

  public SwitchCaseSpec<T> then(String directive) {
    this.then = directive;
    return this;
  }

  @Override
  public void accept(FuncSwitchTaskBuilder.SwitchCasePredicateBuilder switchCasePredicateBuilder) {
    if (this.whenClass != null) {
      switchCasePredicateBuilder.then(this.then).when(this.when, this.whenClass);
    } else {
      switchCasePredicateBuilder.then(this.then).when(this.when);
    }
  }
}
