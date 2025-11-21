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

import dev.langchain4j.agentic.agent.AgentRequest;
import dev.langchain4j.agentic.agent.AgentResponse;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import io.serverlessworkflow.fluent.agentic.AgenticScopedRequest;
import io.serverlessworkflow.fluent.agentic.AgenticScopedResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConditionalAgentServiceImpl<T>
    extends AbstractAgentService<T, ConditionalAgentService<T>>
    implements ConditionalAgentService<T> {

  private final AtomicReference<Consumer<AgenticScopedRequest>> beforeAgentInvocation =
      new AtomicReference<>();
  private final AtomicReference<Consumer<AgenticScopedResponse>> afterAgentInvocation =
      new AtomicReference<>();

  private ConditionalAgentServiceImpl(Class<T> agentServiceClass) {
    super(agentServiceClass);
  }

  public static <T> ConditionalAgentService<T> builder(Class<T> agentServiceClass) {
    return new ConditionalAgentServiceImpl<>(agentServiceClass);
  }

  @Override
  public ConditionalAgentService<T> subAgents(Object... agents) {
    this.workflowBuilder.tasks(
        t ->
            t.sequence(agents)
                .inputFrom(
                    scope -> {
                      if (beforeAgentInvocation.get() != null) {
                        beforeAgentInvocation.get().accept((AgenticScopedRequest) scope);
                      }
                    })
                .outputAs(
                    scope -> {
                      if (afterAgentInvocation.get() != null) {
                        afterAgentInvocation.get().accept((AgenticScopedResponse) scope);
                      }
                    }));
    return this;
  }

  @Override
  public ConditionalAgentService<T> subAgents(List<AgentExecutor> agentExecutors) {
    return this.subAgents(agentExecutors.toArray());
  }

  @Override
  public ConditionalAgentService<T> beforeAgentInvocation(Consumer<AgentRequest> consumer) {
    beforeAgentInvocation.set(
        agenticScopedRequest -> consumer.accept(agenticScopedRequest.asAgentRequest()));
    return this;
  }

  @Override
  public ConditionalAgentService<T> afterAgentInvocation(Consumer<AgentResponse> consumer) {
    afterAgentInvocation.set(
        agenticScopedResponse -> consumer.accept(agenticScopedResponse.asAgentResponse()));
    return this;
  }

  @Override
  public ConditionalAgentService<T> subAgents(Predicate<AgenticScope> condition, Object... agents) {
    this.workflowBuilder.tasks(
        t ->
            Arrays.stream(agents)
                .forEach(
                    agent ->
                        t.when(condition)
                            .agent(agent)
                            .inputFrom(
                                scope -> {
                                  if (beforeAgentInvocation.get() != null) {
                                    beforeAgentInvocation
                                        .get()
                                        .accept((AgenticScopedRequest) scope);
                                  }
                                })
                            .outputAs(
                                scope -> {
                                  if (afterAgentInvocation.get() != null) {
                                    afterAgentInvocation
                                        .get()
                                        .accept((AgenticScopedResponse) scope);
                                  }
                                })));
    return this;
  }

  @Override
  public ConditionalAgentService<T> subAgents(
      Predicate<AgenticScope> condition, List<AgentExecutor> agentExecutors) {
    return this.subAgents(condition, agentExecutors.toArray());
  }

  @Override
  public ConditionalAgentService<T> subAgent(
      Predicate<AgenticScope> condition, AgentExecutor agentExecutor) {
    this.workflowBuilder.tasks(
        t ->
            t.when(condition)
                .agent(agentExecutor)
                .inputFrom(
                    scope -> {
                      if (beforeAgentInvocation.get() != null) {
                        beforeAgentInvocation.get().accept((AgenticScopedRequest) scope);
                      }
                    })
                .outputAs(
                    scope -> {
                      if (afterAgentInvocation.get() != null) {
                        afterAgentInvocation.get().accept((AgenticScopedResponse) scope);
                      }
                    }));
    return this;
  }
}
