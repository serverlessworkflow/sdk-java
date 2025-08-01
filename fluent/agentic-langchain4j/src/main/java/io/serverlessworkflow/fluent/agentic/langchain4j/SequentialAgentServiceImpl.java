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
package io.serverlessworkflow.fluent.agentic.langchain4j;

import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import java.util.List;

public class SequentialAgentServiceImpl<T>
    extends AbstractAgentService<T, SequentialAgentService<T>>
    implements SequentialAgentService<T> {

  private SequentialAgentServiceImpl(Class<T> agentServiceClass) {
    super(agentServiceClass);
  }

  public static <T> SequentialAgentServiceImpl<T> builder(Class<T> agentServiceClass) {
    return new SequentialAgentServiceImpl<>(agentServiceClass);
  }

  @Override
  public SequentialAgentService<T> subAgents(Object... agents) {
    this.workflowBuilder.tasks(t -> t.sequence(agents));
    return this;
  }

  @Override
  public SequentialAgentService<T> subAgents(List<AgentExecutor> agentExecutors) {
    this.subAgents(agentExecutors.toArray());
    return this;
  }
}
