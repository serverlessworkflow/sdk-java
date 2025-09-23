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
import io.serverlessworkflow.fluent.agentic.configurer.AgentTaskConfigurer;
import io.serverlessworkflow.fluent.agentic.configurer.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.agentic.configurer.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AgenticDSL {

  private AgenticDSL() {}

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(
      Function<T, V> function, Class<T> argClass) {
    return f -> f.function(function, argClass);
  }

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function) {
    return f -> f.function(function);
  }

  public static Consumer<FuncSwitchTaskBuilder> cases(SwitchCaseConfigurer... cases) {
    return s -> {
      for (SwitchCaseConfigurer c : cases) {
        s.onPredicate(c);
      }
    };
  }

  public static <T> SwitchCaseSpec<T> on(Predicate<T> when, Class<T> whenClass) {
    return new SwitchCaseSpec<T>().when(when, whenClass);
  }

  public static <T> SwitchCaseSpec<T> on(Predicate<T> when) {
    return new SwitchCaseSpec<T>().when(when);
  }

  public static SwitchCaseConfigurer onDefault(String task) {
    return s -> s.then(task);
  }

  public static SwitchCaseConfigurer onDefault(FlowDirectiveEnum directive) {
    return s -> s.then(directive);
  }

  public static ListenSpec to() {
    return new ListenSpec();
  }

  public static ListenSpec toOne(String type) {
    return new ListenSpec().one(e -> e.type(type));
  }

  public static ListenSpec toAll(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new ListenSpec().all(events);
  }

  public static ListenSpec toAny(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new ListenSpec().any(events);
  }

  public static FuncPredicateEventConfigurer event(String type) {
    return e -> e.type(type);
  }

  // TODO: expand the `event` static ref with more attributes based on community feedback

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function) {
    return event -> event.event(e -> e.type(type).data(function));
  }

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function, Class<T> clazz) {
    return event -> event.event(e -> e.type(type).data(function, clazz));
  }

  // -------- Agentic Workflow Patterns -------- //
  public static AgentTaskConfigurer sequence(Object... agents) {
    return list -> list.sequence(agents);
  }

  public static AgentTaskConfigurer loop(Predicate<AgenticScope> exitCondition, Object... agents) {
    return list -> list.loop(l -> l.subAgents(agents).exitCondition(exitCondition));
  }

  public static AgentTaskConfigurer parallel(Object... agents) {
    return list -> list.parallel(agents);
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
    return list -> list.callFn(fn(function));
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
