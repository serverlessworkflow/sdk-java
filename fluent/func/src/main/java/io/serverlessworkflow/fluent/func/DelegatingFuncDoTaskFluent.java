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

import io.serverlessworkflow.fluent.spec.HasDelegate;
import java.util.function.Consumer;

/**
 * Mixin that implements {@link FuncDoTaskFluent} by forwarding to another instance.
 *
 * @param <SELF> concrete builder type
 */
public interface DelegatingFuncDoTaskFluent<SELF extends DelegatingFuncDoTaskFluent<SELF>>
    extends FuncDoTaskFluent<SELF>, HasDelegate {

  @SuppressWarnings("unchecked")
  default SELF self() {
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  private FuncDoTaskFluent<SELF> d() {
    return (FuncDoTaskFluent<SELF>) this.delegate();
  }

  @Override
  default SELF callFn(String name, Consumer<FuncCallTaskBuilder> cfg) {
    d().callFn(name, cfg);
    return self();
  }

  @Override
  default SELF callFn(Consumer<FuncCallTaskBuilder> cfg) {
    d().callFn(cfg);
    return self();
  }

  @Override
  default SELF forFn(String name, Consumer<FuncForTaskBuilder> cfg) {
    d().forFn(name, cfg);
    return self();
  }

  @Override
  default SELF forFn(Consumer<FuncForTaskBuilder> cfg) {
    d().forFn(cfg);
    return self();
  }

  @Override
  default SELF switchFn(String name, Consumer<FuncSwitchTaskBuilder> cfg) {
    d().switchFn(name, cfg);
    return self();
  }

  @Override
  default SELF switchFn(Consumer<FuncSwitchTaskBuilder> cfg) {
    d().switchFn(cfg);
    return self();
  }
}
