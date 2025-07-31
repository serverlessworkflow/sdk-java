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

import dev.langchain4j.agentic.cognisphere.Cognisphere;
import dev.langchain4j.agentic.internal.AgentExecutor;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import java.util.List;
import java.util.UUID;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class LoopAgentsBuilder {

  private final FuncTaskItemListBuilder funcDelegate;
  private final ForTaskFunction forTask;

  public LoopAgentsBuilder() {
    this.forTask = new ForTaskFunction();
    this.forTask.setFor(new ForTaskConfiguration());
    this.funcDelegate = new FuncTaskItemListBuilder();
  }

  private static <E> void forEachIndexed(List<E> list, ObjIntConsumer<E> consumer) {
    IntStream.range(0, list.size()).forEach(i -> consumer.accept(list.get(i), i));
  }

  public LoopAgentsBuilder subAgents(String baseName, Object... agents) {
    List<AgentExecutor> execs = AgentAdapters.toExecutors(agents);
    forEachIndexed(
        execs,
        (exec, idx) ->
            funcDelegate.callFn(
                baseName + "-" + idx, fn -> fn.function(AgentAdapters.toFunction(exec))));
    return this;
  }

  public LoopAgentsBuilder subAgents(Object... agents) {
    return this.subAgents("agent-" + UUID.randomUUID(), agents);
  }

  public LoopAgentsBuilder maxIterations(int maxIterations) {
    this.forTask.withCollection(ignored -> IntStream.range(0, maxIterations).boxed().toList());
    return this;
  }

  public LoopAgentsBuilder exitCondition(Predicate<Cognisphere> exitCondition) {
    this.forTask.withWhile(AgentAdapters.toWhile(exitCondition), Cognisphere.class);
    return this;
  }

  public ForTaskFunction build() {
    this.forTask.setDo(this.funcDelegate.build());
    return this.forTask;
  }
}
