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
package io.serverlessworkflow.fluent.func.dsl;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.func.dsl.internal.CommonFuncOps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FuncDSL {
  private static final CommonFuncOps OPS = new CommonFuncOps() {};

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(
      Function<T, V> function, Class<T> argClass) {
    return OPS.fn(function, argClass);
  }

  public static <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function) {
    return f -> f.function(function);
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

  public static FuncListenSpec to() {
    return new FuncListenSpec();
  }

  public static FuncListenSpec toOne(String type) {
    return new FuncListenSpec().one(e -> e.type(type));
  }

  public static FuncListenSpec toAll(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new FuncListenSpec().all(events);
  }

  public static FuncListenSpec toAny(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new FuncListenSpec().any(events);
  }

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function) {
    return OPS.event(type, function);
  }

  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function, Class<T> clazz) {
    return OPS.event(type, function, clazz);
  }

  /** Emit a JSON CloudEvent (PojoCloudEventData) from a POJO payload. */
  public static <T> Consumer<FuncEmitTaskBuilder> eventJson(String type, Class<T> clazz) {
    return b -> new FuncEmitSpec().type(type).jsonData(clazz).accept(b);
  }

  public static <T> Consumer<FuncEmitTaskBuilder> eventBytes(
      String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return b -> new FuncEmitSpec().type(type).bytesData(serializer, clazz).accept(b);
  }

  public static Consumer<FuncEmitTaskBuilder> eventBytesUtf8(String type) {
    return b -> new FuncEmitSpec().type(type).bytesDataUtf8().accept(b);
  }

  public static FuncPredicateEventConfigurer event(String type) {
    return OPS.event(type);
  }

  public static <T, R> FuncCallStep<T, R> function(Function<T, R> fn, Class<T> clazz) {
    return new FuncCallStep<>(fn, clazz);
  }

  public static <T, R> FuncCallStep<T, R> function(Function<T, R> fn) {
    Class<T> clazz = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(fn, clazz);
  }

  public static <T, R> FuncCallStep<T, R> function(String name, Function<T, R> fn) {
    Class<T> clazz = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(name, fn, clazz);
  }

  public static <T, R> FuncCallStep<T, R> function(String name, Function<T, R> fn, Class<T> clazz) {
    return new FuncCallStep<>(name, fn, clazz);
  }

  // ------------------  tasks ---------------- //

  public static Consumer<FuncTaskItemListBuilder> tasks(FuncTaskConfigurer... steps) {
    Objects.requireNonNull(steps, "Steps in a tasks are required");
    final List<FuncTaskConfigurer> snapshot = List.of(steps.clone());
    return list -> snapshot.forEach(s -> s.accept(list));
  }

  public static EmitStep emit(Consumer<FuncEmitTaskBuilder> cfg) {
    return new EmitStep(null, cfg);
  }

  public static EmitStep emit(String name, Consumer<FuncEmitTaskBuilder> cfg) {
    return new EmitStep(name, cfg);
  }

  public static <T> EmitStep emit(String type, Function<T, CloudEventData> fn) {
    // `event(type, fn)` is your Consumer<FuncEmitTaskBuilder> for EMIT
    return new EmitStep(null, event(type, fn));
  }

  public static <T> EmitStep emit(String name, String type, Function<T, CloudEventData> fn) {
    return new EmitStep(name, event(type, fn));
  }

  public static <T> EmitStep emit(
      String name, String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return new EmitStep(name, eventBytes(type, serializer, clazz));
  }

  public static <T> EmitStep emit(String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return new EmitStep(null, eventBytes(type, serializer, clazz));
  }

  public static <T> EmitStep emitJson(String type, Class<T> clazz) {
    return new EmitStep(null, eventJson(type, clazz));
  }

  public static <T> EmitStep emitJson(String name, String type, Class<T> clazz) {
    return new EmitStep(name, eventJson(type, clazz));
  }

  public static ListenStep listen(FuncListenSpec spec) {
    return new ListenStep(null, spec);
  }

  public static ListenStep listen(String name, FuncListenSpec spec) {
    return new ListenStep(name, spec);
  }

  public static FuncTaskConfigurer switchCase(
      String taskName, Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(taskName, switchCase);
  }

  public static FuncTaskConfigurer switchCase(Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(switchCase);
  }

  public static FuncTaskConfigurer switchCase(SwitchCaseConfigurer... cases) {
    return switchCase(null, cases);
  }

  public static FuncTaskConfigurer switchCase(String taskName, SwitchCaseConfigurer... cases) {
    Objects.requireNonNull(cases, "cases are required");
    final List<SwitchCaseConfigurer> snapshot = List.of(cases.clone());
    return list -> list.switchCase(taskName, s -> snapshot.forEach(s::onPredicate));
  }

  // Single predicate -> then task
  public static <T> FuncTaskConfigurer switchWhen(Predicate<T> pred, String thenTask) {
    return list -> list.switchCase(cases(caseOf(pred).then(thenTask)));
  }

  // With default directive
  public static <T> FuncTaskConfigurer switchWhenOrElse(
      Predicate<T> pred, String thenTask, FlowDirectiveEnum otherwise) {
    return list ->
        list.switchCase(FuncDSL.cases(caseOf(pred).then(thenTask), caseDefault(otherwise)));
  }

  public static <T> FuncTaskConfigurer switchWhenOrElse(
      Predicate<T> pred, String thenTask, String otherwiseTask) {
    return list ->
        list.switchCase(FuncDSL.cases(caseOf(pred).then(thenTask), caseDefault(otherwiseTask)));
  }

  public static <T> FuncTaskConfigurer forEach(
      Function<T, Collection<?>> collection, Consumer<FuncTaskItemListBuilder> body) {
    return list -> list.forEach(j -> j.collection(collection).tasks(body));
  }

  public static <T> FuncTaskConfigurer forEach(
      Collection<?> collection, Consumer<FuncTaskItemListBuilder> body) {
    Function<T, Collection<?>> f = ctx -> (Collection<?>) collection;
    return list -> list.forEach(j -> j.collection(f).tasks(body));
  }

  // Overload with simple constant collection
  public static <T> FuncTaskConfigurer forEach(
      List<T> collection, Consumer<FuncTaskItemListBuilder> body) {
    return list -> list.forEach(j -> j.collection(ctx -> collection).tasks(body));
  }

  public static FuncTaskConfigurer set(String expr) {
    return list -> list.set(expr);
  }

  public static FuncTaskConfigurer set(Map<String, Object> map) {
    return list -> list.set(s -> s.expr(map));
  }
}
