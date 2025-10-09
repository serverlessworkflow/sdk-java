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
package io.serverlessworkflow.fluent.agentic.dsl;

import dev.langchain4j.agentic.scope.AgenticScope;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.fluent.agentic.AgentDoTaskBuilder;
import io.serverlessworkflow.fluent.agentic.configurers.AgentTaskConfigurer;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.func.dsl.ReflectionUtils;
import io.serverlessworkflow.fluent.func.dsl.SwitchCaseSpec;
import io.serverlessworkflow.fluent.func.dsl.internal.CommonFuncOps;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AgenticDSL {

  private static final CommonFuncOps OPS = new CommonFuncOps() {};

  private AgenticDSL() {}

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(
      Function<T, V> function, Class<T> argClass) {
    return OPS.fn(function, argClass);
  }

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function) {
    return OPS.fn(function);
  }

  public static Consumer<FuncSwitchTaskBuilder> cases(SwitchCaseConfigurer... cases) {
    return OPS.cases(cases);
  }

  public static <T> SwitchCaseSpec<T> caseOf(Predicate<T> when, Class<T> whenClass) {
    return OPS.caseOf(when, whenClass);
  }

  public static <T> SwitchCaseSpec<T> caseOf(Predicate<T> when) {
    return OPS.caseOf(when);
  }

  public static SwitchCaseConfigurer caseDefault(String task) {
    return OPS.caseDefault(task);
  }

  public static SwitchCaseConfigurer caseDefault(FlowDirectiveEnum directive) {
    return OPS.caseDefault(directive);
  }

  public static AgentListenSpec to() {
    return new AgentListenSpec();
  }

  public static AgentListenSpec toOne(String type) {
    return new AgentListenSpec().one(e -> e.type(type));
  }

  public static AgentListenSpec toAll(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new AgentListenSpec().all(events);
  }

  public static AgentListenSpec toAny(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new AgentListenSpec().any(events);
  }

  public static FuncPredicateEventConfigurer event(String type) {
    return OPS.event(type);
  }

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function) {
    return OPS.event(type, function);
  }

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function, Class<T> clazz) {
    return OPS.event(type, function, clazz);
  }

  // -------- Agentic Workflow Patterns -------- //
  public static AgentTaskConfigurer sequence(Object... agents) {
    return list -> list.sequence(agents);
  }

  public static AgentTaskConfigurer sequence(Predicate<?> predicate, Object... agents) {
    return list -> list.sequence(agents).when(predicate);
  }

  public static AgentTaskConfigurer loop(Predicate<AgenticScope> exitCondition, Object... agents) {
    return list -> list.loop(l -> l.subAgents(agents).exitCondition(exitCondition));
  }

  public static AgentTaskConfigurer loop(
      Predicate<AgenticScope> exitCondition, int maxIterations, Object... agents) {
    return list ->
        list.loop(
            l -> l.subAgents(agents).exitCondition(exitCondition).maxIterations(maxIterations));
  }

  public static AgentTaskConfigurer parallel(Object... agents) {
    return list -> list.parallel(agents);
  }

  public static AgentTaskConfigurer parallel(Predicate<?> predicate, Object... agents) {
    return list -> list.parallel(agents).when(predicate);
  }

  // --------- Tasks ------ //
  public static Consumer<AgentDoTaskBuilder> doTasks(AgentTaskConfigurer... steps) {
    Objects.requireNonNull(steps, "Steps in a tasks are required");
    final List<AgentTaskConfigurer> snapshot = List.of(steps.clone());
    return list -> snapshot.forEach(s -> s.accept(list));
  }

  public static <T, V> AgentTaskConfigurer function(Function<T, V> function, Class<T> argClass) {
    return list -> list.callFn(fn(function, argClass));
  }

  public static <T, V> AgentTaskConfigurer function(Function<T, V> function) {
    Class<T> clazz = ReflectionUtils.inferInputType(function);
    return list -> list.callFn(fn(function, clazz));
  }

  public static AgentTaskConfigurer agent(Object agent) {
    return list -> list.agent(agent);
  }

  public static AgentTaskConfigurer conditional(Predicate<?> predicate, Object agent) {
    return list -> list.agent(agent).when(predicate);
  }

  public static AgentTaskConfigurer emit(Consumer<FuncEmitTaskBuilder> event) {
    return list -> list.emit(event);
  }

  public static AgentTaskConfigurer switchCase(Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(switchCase);
  }
}
