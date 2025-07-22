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
    extends DoTaskFluent<SELF, LIST> {

  /** The underlying DoTaskFluent to forward to. */
  DoTaskFluent<?, LIST> internalDelegate();

  /** Convenience cast; implementors just return (SELF) this in practice. */
  @SuppressWarnings("unchecked")
  default SELF self() {
    return (SELF) this;
  }

  /* ---------- Forwarders ---------- */

  @Override
  default SELF set(String name, Consumer<SetTaskBuilder> cfg) {
    internalDelegate().set(name, cfg);
    return self();
  }

  @Override
  default SELF set(Consumer<SetTaskBuilder> cfg) {
    internalDelegate().set(cfg);
    return self();
  }

  @Override
  default SELF set(String name, String expr) {
    internalDelegate().set(name, expr);
    return self();
  }

  @Override
  default SELF set(String expr) {
    internalDelegate().set(expr);
    return self();
  }

  @Override
  default SELF forEach(String name, Consumer<ForTaskBuilder<LIST>> cfg) {
    internalDelegate().forEach(name, cfg);
    return self();
  }

  @Override
  default SELF forEach(Consumer<ForTaskBuilder<LIST>> cfg) {
    internalDelegate().forEach(cfg);
    return self();
  }

  @Override
  default SELF switchCase(String name, Consumer<SwitchTaskBuilder> cfg) {
    internalDelegate().switchCase(name, cfg);
    return self();
  }

  @Override
  default SELF switchCase(Consumer<SwitchTaskBuilder> cfg) {
    internalDelegate().switchCase(cfg);
    return self();
  }

  @Override
  default SELF raise(String name, Consumer<RaiseTaskBuilder> cfg) {
    internalDelegate().raise(name, cfg);
    return self();
  }

  @Override
  default SELF raise(Consumer<RaiseTaskBuilder> cfg) {
    internalDelegate().raise(cfg);
    return self();
  }

  @Override
  default SELF fork(String name, Consumer<ForkTaskBuilder> cfg) {
    internalDelegate().fork(name, cfg);
    return self();
  }

  @Override
  default SELF fork(Consumer<ForkTaskBuilder> cfg) {
    internalDelegate().fork(cfg);
    return self();
  }

  @Override
  default SELF listen(String name, Consumer<ListenTaskBuilder> cfg) {
    internalDelegate().listen(name, cfg);
    return self();
  }

  @Override
  default SELF listen(Consumer<ListenTaskBuilder> cfg) {
    internalDelegate().listen(cfg);
    return self();
  }

  @Override
  default SELF emit(String name, Consumer<EmitTaskBuilder> cfg) {
    internalDelegate().emit(name, cfg);
    return self();
  }

  @Override
  default SELF emit(Consumer<EmitTaskBuilder> cfg) {
    internalDelegate().emit(cfg);
    return self();
  }

  @Override
  default SELF tryCatch(String name, Consumer<TryTaskBuilder<LIST>> cfg) {
    internalDelegate().tryCatch(name, cfg);
    return self();
  }

  @Override
  default SELF tryCatch(Consumer<TryTaskBuilder<LIST>> cfg) {
    internalDelegate().tryCatch(cfg);
    return self();
  }

  @Override
  default SELF callHTTP(String name, Consumer<CallHTTPTaskBuilder> cfg) {
    internalDelegate().callHTTP(name, cfg);
    return self();
  }

  @Override
  default SELF callHTTP(Consumer<CallHTTPTaskBuilder> cfg) {
    internalDelegate().callHTTP(cfg);
    return self();
  }
}
