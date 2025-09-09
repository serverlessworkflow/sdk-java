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

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.agentic.workflow.WorkflowAgentsBuilder;

public class LC4JWorkflowBuilder implements WorkflowAgentsBuilder {

  @Override
  public SequentialAgentService<UntypedAgent> sequenceBuilder() {
    return SequentialAgentServiceImpl.builder(UntypedAgent.class);
  }

  @Override
  public <T> SequentialAgentService<T> sequenceBuilder(Class<T> agentServiceClass) {
    return SequentialAgentServiceImpl.builder(agentServiceClass);
  }

  @Override
  public ParallelAgentService<UntypedAgent> parallelBuilder() {
    return ParallelAgentServiceImpl.builder(UntypedAgent.class);
  }

  @Override
  public <T> ParallelAgentService<T> parallelBuilder(Class<T> agentServiceClass) {
    return ParallelAgentServiceImpl.builder(agentServiceClass);
  }

  @Override
  public LoopAgentService<UntypedAgent> loopBuilder() {
    return LoopAgentServiceImpl.builder(UntypedAgent.class);
  }

  @Override
  public <T> LoopAgentService<T> loopBuilder(Class<T> agentServiceClass) {
    return LoopAgentServiceImpl.builder(agentServiceClass);
  }

  @Override
  public ConditionalAgentService<UntypedAgent> conditionalBuilder() {
    return ConditionalAgentServiceImpl.builder(UntypedAgent.class);
  }

  @Override
  public <T> ConditionalAgentService<T> conditionalBuilder(Class<T> agentServiceClass) {
    return ConditionalAgentServiceImpl.builder(agentServiceClass);
  }
}
