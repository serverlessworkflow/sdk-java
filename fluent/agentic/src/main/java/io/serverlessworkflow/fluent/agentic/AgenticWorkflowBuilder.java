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
package io.serverlessworkflow.fluent.agentic;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.fluent.spec.DocumentBuilder;
import io.serverlessworkflow.fluent.spec.InputBuilder;
import io.serverlessworkflow.fluent.spec.OutputBuilder;
import io.serverlessworkflow.fluent.spec.UseBuilder;
import java.util.function.Consumer;

public final class AgenticWorkflowBuilder {

  private final FuncWorkflowBuilder delegate;

  AgenticWorkflowBuilder(final FuncWorkflowBuilder delegate) {
    this.delegate = delegate;
  }

  public static AgenticWorkflowBuilder workflow() {
    return new AgenticWorkflowBuilder(FuncWorkflowBuilder.workflow());
  }

  public static AgenticWorkflowBuilder workflow(String name) {
    return new AgenticWorkflowBuilder(FuncWorkflowBuilder.workflow(name));
  }

  public static AgenticWorkflowBuilder workflow(String name, String ns) {
    return new AgenticWorkflowBuilder(FuncWorkflowBuilder.workflow(name, ns));
  }

  public AgenticWorkflowBuilder document(Consumer<DocumentBuilder> c) {
    delegate.document(c);
    return this;
  }

  public AgenticWorkflowBuilder input(Consumer<InputBuilder> c) {
    delegate.input(c);
    return this;
  }

  public AgenticWorkflowBuilder output(Consumer<OutputBuilder> c) {
    delegate.output(c);
    return this;
  }

  public AgenticWorkflowBuilder use(Consumer<UseBuilder> c) {
    delegate.use(c);
    return this;
  }

  public AgenticWorkflowBuilder tasks(Consumer<AgentDoTaskBuilder> c) {
    delegate.tasks(
        funcDo -> {
          AgentDoTaskBuilder agentDoTaskBuilder = AgentDoTaskBuilder.wrap(funcDo);
          c.accept(agentDoTaskBuilder);
        });
    return this;
  }

  public Workflow build() {
    return delegate.build();
  }
}
