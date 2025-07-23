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

import io.serverlessworkflow.fluent.func.DelegatingFuncDoTaskFluent;
import io.serverlessworkflow.fluent.spec.HasDelegate;
import java.util.function.Consumer;

public interface DelegatingAgentDoTaskFluent<SELF extends DelegatingAgentDoTaskFluent<SELF>>
    extends AgentDoTaskFluent<SELF>, DelegatingFuncDoTaskFluent<SELF>, HasDelegate {

  @SuppressWarnings("unchecked")
  private AgentDoTaskFluent<SELF> d() {
    return (AgentDoTaskFluent<SELF>) this.delegate();
  }

  @SuppressWarnings("unchecked")
  @Override
  default SELF agent(String name, Object agent) {
    d().agent(name, agent);
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  @Override
  default SELF sequence(String name, Object... agents) {
    d().sequence(name, agents);
    return (SELF) this;
  }

  @SuppressWarnings("unchecked")
  default SELF loop(String name, Consumer<LoopAgentsBuilder> consumer) {
    d().loop(name, consumer);
    return (SELF) this;
  }
}
