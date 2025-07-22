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

import io.serverlessworkflow.fluent.func.FuncDoTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;

public class AgentDoTaskBuilder extends FuncDoTaskBuilder
    implements DelegatingAgentDoTaskFluent<AgentDoTaskBuilder> {

  private AgentDoTaskBuilder(AgentTaskItemListBuilder agentTaskItemListBuilder) {
    super(agentTaskItemListBuilder);
  }

  static AgentDoTaskBuilder wrap(FuncDoTaskBuilder base) {
    FuncTaskItemListBuilder funcList = base.internalDelegate();
    AgentTaskItemListBuilder agentList =
        (funcList instanceof AgentTaskItemListBuilder al)
            ? al
            : new AgentTaskItemListBuilder(funcList.getInternalList());
    return new AgentDoTaskBuilder(agentList);
  }

  @Override
  public AgentDoTaskFluent<?> agentInternalDelegate() {
    return (AgentDoTaskFluent<?>) super.internalDelegate();
  }

  @Override
  public AgentDoTaskBuilder self() {
    return this;
  }
}
