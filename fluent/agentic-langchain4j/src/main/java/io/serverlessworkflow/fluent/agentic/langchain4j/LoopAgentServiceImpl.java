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

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import io.serverlessworkflow.fluent.agentic.LoopAgentsBuilder;
import java.util.List;
import java.util.function.Predicate;

public class LoopAgentServiceImpl<T> extends AbstractAgentService<T, LoopAgentService<T>>
    implements LoopAgentService<T> {

  private final LoopAgentsBuilder loopAgentsBuilder = new LoopAgentsBuilder();

  private LoopAgentServiceImpl(Class<T> agentServiceClass) {
    super(agentServiceClass);
  }

  public static <T> LoopAgentService<T> builder(Class<T> agentServiceClass) {
    return new LoopAgentServiceImpl<>(agentServiceClass);
  }

  @Override
  public LoopAgentService<T> maxIterations(int maxIterations) {
    this.loopAgentsBuilder.maxIterations(maxIterations);
    return this;
  }

  @Override
  public LoopAgentService<T> exitCondition(Predicate<Cognisphere> exitCondition) {
    this.loopAgentsBuilder.exitCondition(exitCondition);
    return this;
  }

  @Override
  public LoopAgentService<T> subAgents(Object... agents) {
    this.loopAgentsBuilder.subAgents(agents);
    this.workflowBuilder.tasks(t -> t.loop(this.loopAgentsBuilder));
    return this;
  }

  @Override
  public LoopAgentService<T> subAgents(List<AgentExecutor> agentExecutors) {
    this.loopAgentsBuilder.subAgents(agentExecutors.toArray());
    this.workflowBuilder.tasks(t -> t.loop(this.loopAgentsBuilder));
    return this;
  }
}
