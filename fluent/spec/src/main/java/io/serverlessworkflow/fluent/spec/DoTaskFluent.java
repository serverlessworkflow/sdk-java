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
 * Documents the exposed fluent `do` DSL.
 *
 * @see <a
 *     href="https://github.com/serverlessworkflow/specification/blob/main/dsl-reference.md#do">CNCF
 *     DSL Reference - Do</a>
 * @param <SELF> The TaskBaseBuilder constructor that sub-tasks will build
 * @param <LIST> The specialized BaseTaskItemListBuilder for sub-tasks that require a list of
 *     sub-tasks, such as `for`.
 */
public interface DoTaskFluent<
    SELF extends DoTaskFluent<SELF, LIST>, LIST extends BaseTaskItemListBuilder<LIST>> {

  SELF set(String name, Consumer<SetTaskBuilder> itemsConfigurer);

  SELF set(Consumer<SetTaskBuilder> itemsConfigurer);

  SELF set(String name, final String expr);

  SELF set(final String expr);

  SELF forEach(String name, Consumer<ForTaskBuilder<LIST>> itemsConfigurer);

  SELF forEach(Consumer<ForTaskBuilder<LIST>> itemsConfigurer);

  SELF switchCase(String name, Consumer<SwitchTaskBuilder> itemsConfigurer);

  SELF switchCase(Consumer<SwitchTaskBuilder> itemsConfigurer);

  SELF raise(String name, Consumer<RaiseTaskBuilder> itemsConfigurer);

  SELF raise(Consumer<RaiseTaskBuilder> itemsConfigurer);

  SELF fork(String name, Consumer<ForkTaskBuilder> itemsConfigurer);

  SELF fork(Consumer<ForkTaskBuilder> itemsConfigurer);

  SELF listen(String name, Consumer<ListenTaskBuilder> itemsConfigurer);

  SELF listen(Consumer<ListenTaskBuilder> itemsConfigurer);

  SELF emit(String name, Consumer<EmitTaskBuilder> itemsConfigurer);

  SELF emit(Consumer<EmitTaskBuilder> itemsConfigurer);

  SELF tryCatch(String name, Consumer<TryTaskBuilder<LIST>> itemsConfigurer);

  SELF tryCatch(Consumer<TryTaskBuilder<LIST>> itemsConfigurer);

  SELF callHTTP(String name, Consumer<CallHTTPTaskBuilder> itemsConfigurer);

  SELF callHTTP(Consumer<CallHTTPTaskBuilder> itemsConfigurer);

  // ----- shortcuts/aliases

  default SELF sc(String name, Consumer<SwitchTaskBuilder> cfg) {
    return switchCase(name, cfg);
  }

  default SELF sc(Consumer<SwitchTaskBuilder> cfg) {
    return switchCase(cfg);
  }

  default SELF tc(String name, Consumer<TryTaskBuilder<LIST>> cfg) {
    return tryCatch(name, cfg);
  }

  default SELF tc(Consumer<TryTaskBuilder<LIST>> cfg) {
    return tryCatch(cfg);
  }
}
