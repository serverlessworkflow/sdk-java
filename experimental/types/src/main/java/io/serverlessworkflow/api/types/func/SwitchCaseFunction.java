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
package io.serverlessworkflow.api.types.func;

import io.serverlessworkflow.api.types.SwitchCase;
import java.util.Optional;
import java.util.function.Predicate;

public class SwitchCaseFunction extends SwitchCase {

  private static final long serialVersionUID = 1L;
  private Predicate<?> predicate;
  private Optional<Class<?>> predicateClass;

  public <T> SwitchCaseFunction withPredicate(Predicate<T> predicate) {
    this.predicate = predicate;
    this.predicateClass = Optional.empty();
    return this;
  }

  public <T> SwitchCaseFunction withPredicate(Predicate<T> predicate, Class<T> predicateClass) {
    this.predicate = predicate;
    this.predicateClass = Optional.ofNullable(predicateClass);
    return this;
  }

  public Predicate<?> predicate() {
    return predicate;
  }

  public Optional<Class<?>> predicateClass() {
    return predicateClass;
  }
}
