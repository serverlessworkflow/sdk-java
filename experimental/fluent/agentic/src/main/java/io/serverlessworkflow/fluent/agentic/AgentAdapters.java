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

import static dev.langchain4j.agentic.internal.AgentUtil.agentsToExecutors;

import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import io.serverlessworkflow.api.types.func.LoopPredicateIndex;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AgentAdapters {

  private AgentAdapters() {}

  public static List<AgentExecutor> toExecutors(Object... agents) {
    return agentsToExecutors(agents);
  }

  public static Function<DefaultAgenticScope, Object> toFunction(
      AgentExecutor exec,
      AtomicReference<Consumer<AgenticScope>> beforeAgentInvocation,
      AtomicReference<Consumer<AgenticScope>> afterAgentInvocation) {
    return defaultAgenticScope -> {
      if (beforeAgentInvocation.get() != null) {
        beforeAgentInvocation.get().accept(defaultAgenticScope);
      }
      Object result = exec.execute(defaultAgenticScope);
      if (afterAgentInvocation.get() != null) {
        defaultAgenticScope.writeState("input", result);
        afterAgentInvocation.get().accept(defaultAgenticScope);
      }
      return result;
    };
  }

  public static LoopPredicateIndex<AgenticScope, Object> toWhile(Predicate<AgenticScope> exit) {
    return (model, item, idx) -> !exit.test(model);
  }

  public static LoopPredicateIndex<AgenticScope, Object> toWhile(
      BiPredicate<AgenticScope, Integer> exit) {
    return (model, item, idx) -> !exit.test(model, idx);
  }
}
