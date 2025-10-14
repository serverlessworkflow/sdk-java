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

import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import java.util.Objects;
import java.util.function.Consumer;

/** Chainable emit step; applies FuncEmitSpec then queued export/when. */
public final class EmitStep extends Step<EmitStep, FuncEmitTaskBuilder> {

  private final String name; // nullable
  private final Consumer<FuncEmitTaskBuilder> cfg;

  EmitStep(String name, Consumer<FuncEmitTaskBuilder> cfg) {
    this.name = name;
    this.cfg = Objects.requireNonNull(cfg, "cfg");
  }

  @Override
  protected void configure(FuncTaskItemListBuilder list, Consumer<FuncEmitTaskBuilder> postApply) {
    if (name == null) {
      list.emit(
          e -> {
            cfg.accept(e);
            postApply.accept(e);
          });
    } else {
      list.emit(
          name,
          e -> {
            cfg.accept(e);
            postApply.accept(e);
          });
    }
  }
}
