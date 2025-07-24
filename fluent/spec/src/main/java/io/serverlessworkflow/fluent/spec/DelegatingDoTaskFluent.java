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

import java.util.function.Consumer;

/**
 * Mixin that implements {@link DoTaskFluent} by delegating to another instance.
 *
 * @param <SELF> the concrete delegating type
 * @param <LIST> the list-builder type used by nested constructs like for/try
 */
public interface DelegatingDoTaskFluent<
        SELF extends DelegatingDoTaskFluent<SELF, LIST>, LIST extends BaseTaskItemListBuilder<LIST>>
    extends DoTaskFluent<SELF, LIST>, HasDelegate {

  @SuppressWarnings("unchecked")
  default SELF self() {
    return (SELF) this;
  }

  LIST list();

  @SuppressWarnings("unchecked")
  private DoTaskFluent<SELF, LIST> d() {
    return (DoTaskFluent<SELF, LIST>) this.delegate();
  }

  /* ---------- Forwarders ---------- */

  @Override
  default SELF set(String name, Consumer<SetTaskBuilder> cfg) {
    d().set(name, cfg);
    return self();
  }

  @Override
  default SELF set(Consumer<SetTaskBuilder> cfg) {
    d().set(cfg);
    return self();
  }

  @Override
  default SELF set(String name, String expr) {
    d().set(name, expr);
    return self();
  }

  @Override
  default SELF set(String expr) {
    d().set(expr);
    return self();
  }

  @Override
  default SELF forEach(String name, Consumer<ForTaskBuilder<LIST>> cfg) {
    d().forEach(name, cfg);
    return self();
  }

  @Override
  default SELF forEach(Consumer<ForTaskBuilder<LIST>> cfg) {
    d().forEach(cfg);
    return self();
  }

  @Override
  default SELF switchCase(String name, Consumer<SwitchTaskBuilder> cfg) {
    d().switchCase(name, cfg);
    return self();
  }

  @Override
  default SELF switchCase(Consumer<SwitchTaskBuilder> cfg) {
    d().switchCase(cfg);
    return self();
  }

  @Override
  default SELF raise(String name, Consumer<RaiseTaskBuilder> cfg) {
    d().raise(name, cfg);
    return self();
  }

  @Override
  default SELF raise(Consumer<RaiseTaskBuilder> cfg) {
    d().raise(cfg);
    return self();
  }

  @Override
  default SELF fork(String name, Consumer<ForkTaskBuilder> cfg) {
    d().fork(name, cfg);
    return self();
  }

  @Override
  default SELF fork(Consumer<ForkTaskBuilder> cfg) {
    d().fork(cfg);
    return self();
  }

  @Override
  default SELF listen(String name, Consumer<ListenTaskBuilder> cfg) {
    d().listen(name, cfg);
    return self();
  }

  @Override
  default SELF listen(Consumer<ListenTaskBuilder> cfg) {
    d().listen(cfg);
    return self();
  }

  @Override
  default SELF emit(String name, Consumer<EmitTaskBuilder> cfg) {
    d().emit(name, cfg);
    return self();
  }

  @Override
  default SELF emit(Consumer<EmitTaskBuilder> cfg) {
    d().emit(cfg);
    return self();
  }

  @Override
  default SELF tryCatch(String name, Consumer<TryTaskBuilder<LIST>> cfg) {
    d().tryCatch(name, cfg);
    return self();
  }

  @Override
  default SELF tryCatch(Consumer<TryTaskBuilder<LIST>> cfg) {
    d().tryCatch(cfg);
    return self();
  }

  @Override
  default SELF callHTTP(String name, Consumer<CallHTTPTaskBuilder> cfg) {
    d().callHTTP(name, cfg);
    return self();
  }

  @Override
  default SELF callHTTP(Consumer<CallHTTPTaskBuilder> cfg) {
    d().callHTTP(cfg);
    return self();
  }
}
