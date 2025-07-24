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
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.BaseTaskItemListBuilder;
import java.util.List;
import java.util.function.Consumer;

public class AgentTaskItemListBuilder extends BaseTaskItemListBuilder<AgentTaskItemListBuilder>
    implements AgentDoTaskFluent<AgentTaskItemListBuilder> {

  private final FuncTaskItemListBuilder funcDelegate;

  AgentTaskItemListBuilder() {
    super();
    this.funcDelegate = new FuncTaskItemListBuilder(super.mutableList());
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
                this.funcDelegate.callFn(name, fn -> fn.function(AgentAdapters.toFunction(exec))));
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
    this.addTaskItem(new TaskItem(name, new Task().withForTask(builder.build())));
    return self();
  }

  @Override
  public AgentTaskItemListBuilder parallel(String name, Object... agents) {
    this.funcDelegate.forkFn(
        name,
        fork -> {
          List<AgentExecutor> execs = AgentAdapters.toExecutors(agents);
          for (int i = 0; i < execs.size(); i++) {
            AgentExecutor ex = execs.get(i);
            fork.branch("branch-" + i + "-" + name, AgentAdapters.toFunction(ex));
          }
        });
    return self();
  }

  @Override
  public AgentTaskItemListBuilder self() {
    return this;
  }
}
