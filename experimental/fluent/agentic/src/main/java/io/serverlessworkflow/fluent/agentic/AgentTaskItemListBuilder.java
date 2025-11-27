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

import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.agentic.spi.AgentDoFluent;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncForkTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSetTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.BaseTaskItemListBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AgentTaskItemListBuilder extends BaseTaskItemListBuilder<AgentTaskItemListBuilder>
    implements AgentDoFluent<AgentTaskItemListBuilder> {

  private final FuncTaskItemListBuilder delegate;
  private final AtomicReference<Consumer<AgenticScopedRequest>> beforeAgentInvocation =
      new AtomicReference<>();
  private final AtomicReference<Consumer<AgenticScopedResponse>> afterAgentInvocation =
      new AtomicReference<>();

  public AgentTaskItemListBuilder() {
    super();
    this.delegate = new FuncTaskItemListBuilder(super.mutableList());
  }

  @Override
  protected AgentTaskItemListBuilder self() {
    return this;
  }

  @Override
  protected AgentTaskItemListBuilder newItemListBuilder() {
    return new AgentTaskItemListBuilder();
  }

  @Override
  public AgentTaskItemListBuilder agent(String name, Object agent) {
    AgentAdapters.toExecutors(agent)
        .forEach(
            exec ->
                this.delegate.function(
                    name,
                    fn ->
                        fn.function(
                            AgentAdapters.toFunction(
                                exec, beforeAgentInvocation, afterAgentInvocation),
                            DefaultAgenticScope.class)));
    return self();
  }

  @Override
  public AgentTaskItemListBuilder sequence(String name, Object... agents) {
    for (int i = 0; i < agents.length; i++) {
      agent(name + "-" + i, agents[i]);
    }
    return self();
  }

  @Override
  public AgentTaskItemListBuilder loop(String name, Consumer<LoopAgentsBuilder> consumer) {
    final LoopAgentsBuilder builder = new LoopAgentsBuilder();
    consumer.accept(builder);
    this.loop(name, builder);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder loop(String name, LoopAgentsBuilder builder) {
    this.addTaskItem(new TaskItem(name, new Task().withForTask(builder.build())));
    return self();
  }

  @Override
  public AgentTaskItemListBuilder parallel(String name, Object... agents) {
    this.delegate.fork(
        name,
        fork -> {
          List<AgentExecutor> execs = AgentAdapters.toExecutors(agents);
          for (int i = 0; i < execs.size(); i++) {
            AgentExecutor ex = execs.get(i);
            String agentName =
                ex.agentInvoker().name() != null
                    ? ex.agentInvoker().name()
                    : "branch-" + i + "-" + name;
            fork.branch(
                agentName,
                AgentAdapters.toFunction(ex, beforeAgentInvocation, afterAgentInvocation),
                DefaultAgenticScope.class);
          }
        });
    return self();
  }

  @Override
  public AgentTaskItemListBuilder function(String name, Consumer<FuncCallTaskBuilder> cfg) {
    this.delegate.function(name, cfg);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder emit(String name, Consumer<FuncEmitTaskBuilder> itemsConfigurer) {
    this.delegate.emit(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder listen(
      String name, Consumer<AgentListenTaskBuilder> itemsConfigurer) {
    final AgentListenTaskBuilder builder = new AgentListenTaskBuilder();
    itemsConfigurer.accept(builder);
    this.addTaskItem(new TaskItem(name, new Task().withListenTask(builder.build())));
    return self();
  }

  @Override
  public AgentTaskItemListBuilder forEach(
      String name, Consumer<FuncForTaskBuilder> itemsConfigurer) {
    this.delegate.forEach(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder fork(String name, Consumer<FuncForkTaskBuilder> itemsConfigurer) {
    this.delegate.fork(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder set(String name, Consumer<FuncSetTaskBuilder> itemsConfigurer) {
    this.delegate.set(name, itemsConfigurer);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder set(String name, String expr) {
    this.delegate.set(name, expr);
    return self();
  }

  @Override
  public AgentTaskItemListBuilder switchCase(
      String name, Consumer<FuncSwitchTaskBuilder> itemsConfigurer) {
    this.delegate.switchCase(name, itemsConfigurer);
    return self();
  }

  public AgentTaskItemListBuilder inputFrom(Consumer<AgenticScope> beforeAgentInvocation) {
    this.beforeAgentInvocation.set(beforeAgentInvocation::accept);
    return self();
  }

  public AgentTaskItemListBuilder outputAs(Consumer<AgenticScope> afterAgentInvocationConsumer) {
    this.afterAgentInvocation.set(afterAgentInvocationConsumer::accept);
    return self();
  }
}
