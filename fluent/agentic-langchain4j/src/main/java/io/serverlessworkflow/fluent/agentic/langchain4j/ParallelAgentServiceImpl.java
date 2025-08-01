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
import dev.langchain4j.agentic.workflow.ParallelAgentService;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ParallelAgentServiceImpl<T> extends AbstractAgentService<T, ParallelAgentService<T>>
    implements ParallelAgentService<T> {

  private ParallelAgentServiceImpl(Class<T> agentServiceClass) {
    super(agentServiceClass);
  }

  public static <T> ParallelAgentService<T> builder(Class<T> agentServiceClass) {
    return new ParallelAgentServiceImpl<>(agentServiceClass);
  }

  @Override
  public ParallelAgentService<T> executorService(ExecutorService executorService) {
    this.workflowExecBuilder.withExecutorFactory(() -> executorService);
    return this;
  }

  @Override
  public ParallelAgentService<T> subAgents(Object... agents) {
    this.workflowBuilder.tasks(t -> t.parallel(agents));
    return this;
  }

  @Override
  public ParallelAgentService<T> subAgents(List<AgentExecutor> agentExecutors) {
    return this.subAgents(agentExecutors.toArray());
  }
}
