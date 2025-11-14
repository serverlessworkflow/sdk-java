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
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.api.types.func.JavaFilterFunction;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncCallHttpConfigurer;
import io.serverlessworkflow.fluent.func.configurers.FuncPredicateEventConfigurer;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.func.dsl.internal.CommonFuncOps;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent, ergonomic shortcuts for building function-centric workflows.
 *
 * <p>This DSL wraps the lower-level builders with strongly-typed helpers that:
 *
 * <ul>
 *   <li>Infer input types for Java lambdas where possible.
 *   <li>Expose chainable steps (e.g., {@code emit(...).exportAs(...).when(...)}) via {@code Step}s.
 *   <li>Provide opinionated helpers for CloudEvents (JSON/bytes) and listen strategies.
 *   <li>Offer context-aware function variants ({@code withContext}, {@code withInstanceId}, {@code
 *       agent}).
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * Workflow wf = FuncWorkflowBuilder.workflow()
 *   .tasks(
 *     FuncDSL.function(String::trim, String.class),
 *     FuncDSL.emitJson("org.acme.started", MyPayload.class),
 *     FuncDSL.listen(FuncDSL.toAny("type.one", "type.two"))
 *       .outputAs(map -> map.get("value")),
 *     FuncDSL.switchWhenOrElse((Integer v) -> v > 0, "positive", FlowDirectiveEnum.END)
 *   ).build();
 * }</pre>
 */
public final class FuncDSL {
  private static final CommonFuncOps OPS = new CommonFuncOps() {};

  private FuncDSL() {}

  /**
   * Create a builder configurer for a {@link FuncCallTaskBuilder} that invokes a plain Java {@link
   * Function} with an explicit input type.
   *
   * @param function the function to call during task execution
   * @param argClass the expected input class for type-safe model conversion
   * @param <T> input type
   * @param <V> output type
   * @return a consumer that configures a {@code FuncCallTaskBuilder}
   */
  public static <T, V> Consumer<FuncCallTaskBuilder> fn(
      Function<T, V> function, Class<T> argClass) {
    return OPS.fn(function, argClass);
  }

  /**
   * Create a builder configurer for a {@link FuncCallTaskBuilder} that invokes a plain Java {@link
   * Function}. The input type is inferred when possible.
   *
   * @param function the function to call during task execution
   * @param <T> input type
   * @param <V> output type
   * @return a consumer that configures a {@code FuncCallTaskBuilder}
   */
  public static <T, V> Consumer<FuncCallTaskBuilder> fn(Function<T, V> function) {
    return f -> f.function(function);
  }

  /**
   * Compose multiple switch cases into a single configurer for {@link FuncSwitchTaskBuilder}.
   *
   * @param cases one or more {@link SwitchCaseConfigurer} built via {@link #caseOf(Predicate)} or
   *     {@link #caseDefault(String)}
   * @return a consumer to apply on a switch task builder
   */
  public static Consumer<FuncSwitchTaskBuilder> cases(SwitchCaseConfigurer... cases) {
    return OPS.cases(cases);
  }

  /**
   * Start a typed switch case using a Java {@link Predicate} with explicit input type.
   *
   * @param when the predicate used to match this case
   * @param whenClass the predicate input class (used for typed conversion)
   * @param <T> predicate input type
   * @return a fluent builder to set the consequent action (e.g., {@code then("taskName")})
   */
  public static <T> SwitchCaseSpec<T> caseOf(Predicate<T> when, Class<T> whenClass) {
    return OPS.caseOf(when, whenClass);
  }

  /**
   * Start a switch case using a Java {@link Predicate}. Type inference is used when possible.
   *
   * @param when the predicate used to match this case
   * @param <T> predicate input type
   * @return a fluent builder to set the consequent action (e.g., {@code then("taskName")})
   */
  public static <T> SwitchCaseSpec<T> caseOf(Predicate<T> when) {
    return OPS.caseOf(when);
  }

  /**
   * Default branch for a switch that jumps to a task name.
   *
   * @param task task name to continue with when no predicate matches
   * @return switch case configurer
   */
  public static SwitchCaseConfigurer caseDefault(String task) {
    return OPS.caseDefault(task);
  }

  /**
   * Default branch for a switch that uses a {@link FlowDirectiveEnum} (e.g. {@code END}, {@code
   * CONTINUE}).
   *
   * @param directive fallback directive when no predicate matches
   * @return switch case configurer
   */
  public static SwitchCaseConfigurer caseDefault(FlowDirectiveEnum directive) {
    return OPS.caseDefault(directive);
  }

  /**
   * Begin building a {@code listen} specification with no pre-selected strategy.
   *
   * @return a new {@link FuncListenSpec}
   */
  public static FuncListenSpec to() {
    return new FuncListenSpec();
  }

  /**
   * Convenience to listen for exactly one event type.
   *
   * @param type CloudEvent type to listen for
   * @return a {@link FuncListenSpec} set to {@code one(type)}
   */
  public static FuncListenSpec toOne(String type) {
    return new FuncListenSpec().one(e -> e.type(type));
  }

  /**
   * Convenience to listen for all the given event types (logical AND).
   *
   * @param types CloudEvent types
   * @return a {@link FuncListenSpec} set to {@code all(types...)}
   */
  public static FuncListenSpec toAll(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new FuncListenSpec().all(events);
  }

  /**
   * Convenience to listen for any of the given event types (logical OR).
   *
   * @param types CloudEvent types
   * @return a {@link FuncListenSpec} set to {@code any(types...)}
   */
  public static FuncListenSpec toAny(String... types) {
    FuncPredicateEventConfigurer[] events = new FuncPredicateEventConfigurer[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = event(types[i]);
    }
    return new FuncListenSpec().any(events);
  }

  /**
   * Build an {@code emit} event configurer by supplying a type and a function that produces {@link
   * CloudEventData}. The function is invoked at runtime with the current payload.
   *
   * @param type CloudEvent type
   * @param function function that maps workflow input to {@link CloudEventData}
   * @param <T> input type to the function
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function) {
    return OPS.event(type, function);
  }

  /**
   * Same as {@link #event(String, Function)} but with an explicit input class to guide conversion.
   *
   * @param type CloudEvent type
   * @param function function that maps workflow input to {@link CloudEventData}
   * @param clazz expected input class for conversion
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> event(
      String type, Function<T, CloudEventData> function, Class<T> clazz) {
    return OPS.event(type, function, clazz);
  }

  /**
   * Emit a JSON CloudEvent for a POJO input type. Sets {@code contentType=application/json} and
   * serializes the input to bytes using the configured JSON mapper.
   *
   * @param type CloudEvent type
   * @param clazz input POJO class (used for typing and conversion)
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> eventJson(String type, Class<T> clazz) {
    return b -> new FuncEmitSpec().type(type).jsonData(clazz).accept(b);
  }

  /**
   * Emit a CloudEvent with arbitrary bytes payload generated by a custom serializer.
   *
   * @param type CloudEvent type
   * @param serializer function producing bytes from the input
   * @param clazz expected input class for conversion
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> eventBytes(
      String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return b -> new FuncEmitSpec().type(type).bytesData(serializer, clazz).accept(b);
  }

  /**
   * Emit a CloudEvent where the input model is serialized to bytes using UTF-8. Useful for
   * string-like payloads (e.g., already-built JSON or text).
   *
   * @param type CloudEvent type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static Consumer<FuncEmitTaskBuilder> eventBytesUtf8(String type) {
    return b -> new FuncEmitSpec().type(type).bytesDataUtf8().accept(b);
  }

  /**
   * Create a predicate event configurer for {@code listen} specs.
   *
   * @param type CloudEvent type
   * @return predicate event configurer for use in {@link FuncListenSpec}
   */
  public static FuncPredicateEventConfigurer event(String type) {
    return OPS.event(type);
  }

  /**
   * Create a {@link FuncCallStep} that calls a simple Java {@link Function} with explicit input
   * type.
   *
   * @param fn the function to execute at runtime
   * @param clazz expected input class for model conversion
   * @param <T> input type
   * @param <R> result type
   * @return a call step which supports chaining (e.g., {@code .exportAs(...).when(...)})
   */
  public static <T, R> FuncCallStep<T, R> function(Function<T, R> fn, Class<T> clazz) {
    return new FuncCallStep<>(fn, clazz);
  }

  /**
   * Build a call step for functions that need {@link WorkflowContextData} as the first parameter.
   * The DSL wraps it as a {@link JavaContextFunction} and injects the runtime context.
   *
   * <p>Signature expected: {@code (ctx, payload) -> result}
   *
   * @param fn context-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withContext(JavaContextFunction<T, R> fn, Class<T> in) {
    return withContext(null, fn, in);
  }

  /**
   * Build a call step for functions that expect the workflow instance ID as the first parameter.
   * The instance ID is extracted from the runtime context.
   *
   * <p>Signature expected: {@code (instanceId, payload) -> result}
   *
   * @param fn instance-id-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withInstanceId(
      InstanceIdBiFunction<T, R> fn, Class<T> in) {
    return withInstanceId(null, fn, in);
  }

  /**
   * Named variant of {@link #withContext(JavaContextFunction, Class)}.
   *
   * @param name task name
   * @param fn context-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withContext(
      String name, JavaContextFunction<T, R> fn, Class<T> in) {
    return new FuncCallStep<>(name, fn, in);
  }

  /**
   * Build a call step for functions that need {@link WorkflowContextData} and {@link
   * io.serverlessworkflow.impl.TaskContextData} as the first and second parameter. The DSL wraps it
   * as a {@link JavaFilterFunction} and injects the runtime context.
   *
   * <p>Signature expected: {@code (wctx, tctx, payload) -> result}
   *
   * @param fn context-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withFilter(JavaFilterFunction<T, R> fn, Class<T> in) {
    return withFilter(null, fn, in);
  }

  /**
   * Named variant of {@link #withFilter(JavaFilterFunction, Class)}.
   *
   * @param name task name
   * @param fn context-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withFilter(
      String name, JavaFilterFunction<T, R> fn, Class<T> in) {
    return new FuncCallStep<>(name, fn, in);
  }

  /**
   * Named variant of {@link #withInstanceId(InstanceIdBiFunction, Class)}.
   *
   * @param name task name
   * @param fn instance-id-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withInstanceId(
      String name, InstanceIdBiFunction<T, R> fn, Class<T> in) {
    JavaContextFunction<T, R> jcf = (payload, wctx) -> fn.apply(wctx.instanceData().id(), payload);
    return new FuncCallStep<>(name, jcf, in);
  }

  /**
   * Builds a composition of the current workflow instance id and the definition of the task
   * position as a JSON pointer.
   */
  static String defaultUniqueId(WorkflowContextData wctx, TaskContextData tctx) {
    return String.format("%s-%s", wctx.instanceData().id(), tctx.position().jsonPointer());
  }

  /**
   * Build a call step for functions that expect a composition with the workflow instance id and the
   * task position as the first parameter. The instance ID is extracted from the runtime context,
   * the task position from the definition.
   *
   * <p>Signature expected: {@code (uniqueId, payload) -> result}
   *
   * @param fn unique-id-aware bi-function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withUniqueId(
      String name, UniqueIdBiFunction<T, R> fn, Class<T> in) {
    JavaFilterFunction<T, R> jff =
        (payload, wctx, tctx) -> fn.apply(defaultUniqueId(wctx, tctx), payload);
    return new FuncCallStep<>(name, jff, in);
  }

  public static <T, R> FuncCallStep<T, R> withUniqueId(UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(null, fn, in);
  }

  /**
   * Create a fire-and-forget side-effect step (unnamed). The consumer receives the typed input.
   *
   * @param consumer side-effect function
   * @param clazz expected input class for conversion
   * @param <T> input type
   * @return a consume step
   */
  public static <T> ConsumeStep<T> consume(Consumer<T> consumer, Class<T> clazz) {
    return new ConsumeStep<>(consumer, clazz);
  }

  /**
   * Named variant of {@link #consume(Consumer, Class)}.
   *
   * @param name task name
   * @param consumer side-effect function
   * @param clazz expected input class
   * @param <T> input type
   * @return a consume step
   */
  public static <T> ConsumeStep<T> consume(String name, Consumer<T> consumer, Class<T> clazz) {
    return new ConsumeStep<>(name, consumer, clazz);
  }

  /**
   * Agent-style sugar for methods that receive a "memory id" as first parameter. We reuse the
   * workflow instance id for that purpose.
   *
   * <p>Equivalent to {@link #withInstanceId(InstanceIdBiFunction, Class)}.
   *
   * @param fn (instanceId, payload) -> result
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> agent(UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(fn, in);
  }

  /**
   * Named agent-style sugar. See {@link #agent(UniqueIdBiFunction, Class)}.
   *
   * @param name task name
   * @param fn (instanceId, payload) -> result
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> agent(
      String name, UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(name, fn, in);
  }

  /**
   * Create a {@link FuncCallStep} that invokes a plain Java {@link Function} with inferred input
   * type.
   *
   * @param fn the function to execute
   * @param <T> input type
   * @param <R> output type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> function(Function<T, R> fn) {
    Class<T> clazz = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(fn, clazz);
  }

  /**
   * Named variant of {@link #function(Function)} with inferred input type.
   *
   * @param name task name
   * @param fn the function to execute
   * @param <T> input type
   * @param <R> output type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> function(String name, Function<T, R> fn) {
    Class<T> clazz = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(name, fn, clazz);
  }

  /**
   * Named variant of {@link #function(Function, Class)} with explicit input type.
   *
   * @param name task name
   * @param fn the function to execute
   * @param clazz expected input class
   * @param <T> input type
   * @param <R> output type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> function(String name, Function<T, R> fn, Class<T> clazz) {
    return new FuncCallStep<>(name, fn, clazz);
  }

  // ------------------  tasks ---------------- //

  /**
   * Compose a list of step configurers into a single {@code tasks} block. Preserves order and
   * defers application to the underlying list builder.
   *
   * @param steps one or more step configurers (including {@link Step} subclasses)
   * @return a consumer for {@link FuncTaskItemListBuilder}
   */
  public static Consumer<FuncTaskItemListBuilder> tasks(FuncTaskConfigurer... steps) {
    Objects.requireNonNull(steps, "Steps in a tasks are required");
    final List<FuncTaskConfigurer> snapshot = List.of(steps.clone());
    return list -> snapshot.forEach(s -> s.accept(list));
  }

  /**
   * Create an {@code emit} step from a low-level {@link FuncEmitTaskBuilder} configurer. Prefer
   * higher-level helpers like {@link #emitJson(String, Class)} where possible.
   *
   * @param cfg emit builder configurer
   * @return an {@link EmitStep} supporting chaining (e.g., {@code .exportAs(...).when(...)})
   */
  public static EmitStep emit(Consumer<FuncEmitTaskBuilder> cfg) {
    return new EmitStep(null, cfg);
  }

  /**
   * Named variant of {@link #emit(Consumer)}.
   *
   * @param name task name
   * @param cfg emit builder configurer
   * @return a named emit step
   */
  public static EmitStep emit(String name, Consumer<FuncEmitTaskBuilder> cfg) {
    return new EmitStep(name, cfg);
  }

  /**
   * Convenience for emitting a CloudEvent using a function that builds {@link CloudEventData}.
   *
   * @param type CloudEvent type
   * @param fn function that produces event data from the input
   * @param <T> input type
   * @return an {@link EmitStep}
   */
  public static <T> EmitStep emit(String type, Function<T, CloudEventData> fn) {
    return new EmitStep(null, event(type, fn));
  }

  /**
   * Named variant of {@link #emit(String, Function)}.
   *
   * @param name task name
   * @param type CloudEvent type
   * @param fn function producing {@link CloudEventData}
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emit(String name, String type, Function<T, CloudEventData> fn) {
    return new EmitStep(name, event(type, fn));
  }

  /**
   * Emit a bytes-based CloudEvent using a custom serializer and explicit input class.
   *
   * @param name task name
   * @param type CloudEvent type
   * @param serializer function producing bytes
   * @param clazz expected input class
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emit(
      String name, String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return new EmitStep(name, eventBytes(type, serializer, clazz));
  }

  /** Unnamed variant of {@link #emit(String, String, Function, Class)}. */
  public static <T> EmitStep emit(String type, Function<T, byte[]> serializer, Class<T> clazz) {
    return new EmitStep(null, eventBytes(type, serializer, clazz));
  }

  /**
   * Emit a JSON CloudEvent from a POJO input class (unnamed).
   *
   * @param type CloudEvent type
   * @param clazz input POJO class
   * @param <T> input type
   * @return an {@link EmitStep}
   */
  public static <T> EmitStep emitJson(String type, Class<T> clazz) {
    return new EmitStep(null, eventJson(type, clazz));
  }

  /**
   * Emit a JSON CloudEvent from a POJO input class (named).
   *
   * @param name task name
   * @param type CloudEvent type
   * @param clazz input POJO class
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emitJson(String name, String type, Class<T> clazz) {
    return new EmitStep(name, eventJson(type, clazz));
  }

  /**
   * Create a {@code listen} step from a {@link FuncListenSpec}.
   *
   * @param spec a listen spec (e.g., {@code toAny("a","b")}, {@code toOne("x")})
   * @return a {@link ListenStep} supporting chaining (e.g., {@code .outputAs(...).when(...)})
   */
  public static ListenStep listen(FuncListenSpec spec) {
    return new ListenStep(null, spec);
  }

  /**
   * Named variant of {@link #listen(FuncListenSpec)}.
   *
   * @param name task name
   * @param spec listen spec
   * @return a named {@link ListenStep}
   */
  public static ListenStep listen(String name, FuncListenSpec spec) {
    return new ListenStep(name, spec);
  }

  /**
   * Low-level switch case configurer using a custom builder consumer. Prefer the {@link
   * #caseOf(Predicate)} helpers when possible.
   *
   * @param taskName optional task name
   * @param switchCase consumer to configure the {@link FuncSwitchTaskBuilder}
   * @return a list configurer
   */
  public static FuncTaskConfigurer switchCase(
      String taskName, Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(taskName, switchCase);
  }

  /** Variant of {@link #switchCase(String, Consumer)} without a name. */
  public static FuncTaskConfigurer switchCase(Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(switchCase);
  }

  /**
   * Convenience to apply multiple {@link SwitchCaseConfigurer} built via {@link
   * #caseOf(Predicate)}.
   *
   * @param cases case configurers
   * @return list configurer
   */
  public static FuncTaskConfigurer switchCase(SwitchCaseConfigurer... cases) {
    return switchCase(null, cases);
  }

  /**
   * Named variant of {@link #switchCase(SwitchCaseConfigurer...)}.
   *
   * @param taskName task name
   * @param cases case configurers
   * @return list configurer
   */
  public static FuncTaskConfigurer switchCase(String taskName, SwitchCaseConfigurer... cases) {
    Objects.requireNonNull(cases, "cases are required");
    final List<SwitchCaseConfigurer> snapshot = List.of(cases.clone());
    return list -> list.switchCase(taskName, s -> snapshot.forEach(s::onPredicate));
  }

  /**
   * Sugar for a single-case switch: if predicate matches, jump to {@code thenTask}.
   *
   * @param pred predicate
   * @param thenTask task name when predicate is true
   * @param predClass predicate class
   * @param <T> predicate input type
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer switchWhen(
      Predicate<T> pred, String thenTask, Class<T> predClass) {
    return list -> list.switchCase(cases(caseOf(pred, predClass).then(thenTask)));
  }

  /**
   * JQ-based condition: if the JQ expression evaluates truthy → jump to {@code thenTask}.
   *
   * <pre>
   *   switchWhen(".approved == true", "approveOrder")
   * </pre>
   *
   * <p>The JQ expression is evaluated against the task input at runtime. When the predicate is
   * false, the workflow follows the default flow directive for the switch task (as defined by the
   * underlying implementation / spec).
   *
   * @param jqExpression JQ expression evaluated against the current task input
   * @param thenTask task name to jump to when the expression evaluates truthy
   * @return list configurer
   */
  public static FuncTaskConfigurer switchWhen(String jqExpression, String thenTask) {
    return list -> list.switchCase(sw -> sw.on(c -> c.when(jqExpression).then(thenTask)));
  }

  /**
   * Sugar for a single-case switch with a default {@link FlowDirectiveEnum} fallback.
   *
   * @param pred predicate
   * @param thenTask task name when predicate is true
   * @param otherwise default flow directive when predicate is false
   * @param predClass predicate class
   * @param <T> predicate input type
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer switchWhenOrElse(
      Predicate<T> pred, String thenTask, FlowDirectiveEnum otherwise, Class<T> predClass) {
    return list ->
        list.switchCase(
            FuncDSL.cases(caseOf(pred, predClass).then(thenTask), caseDefault(otherwise)));
  }

  /**
   * Sugar for a single-case switch with a default task fallback.
   *
   * @param pred predicate
   * @param thenTask task name when predicate is true
   * @param otherwiseTask task name when predicate is false
   * @param predClass predicate class
   * @param <T> predicate input type
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer switchWhenOrElse(
      Predicate<T> pred, String thenTask, String otherwiseTask, Class<T> predClass) {
    return list ->
        list.switchCase(cases(caseOf(pred, predClass).then(thenTask), caseDefault(otherwiseTask)));
  }

  /**
   * JQ-based condition: if the JQ expression evaluates truthy → jump to {@code thenTask}, otherwise
   * follow the {@link FlowDirectiveEnum} given in {@code otherwise}.
   *
   * <pre>
   *   switchWhenOrElse(".approved == true", "sendEmail", FlowDirectiveEnum.END)
   * </pre>
   *
   * <p>The JQ expression is evaluated against the task input at runtime.
   */
  public static FuncTaskConfigurer switchWhenOrElse(
      String jqExpression, String thenTask, FlowDirectiveEnum otherwise) {

    Objects.requireNonNull(jqExpression, "jqExpression");
    Objects.requireNonNull(thenTask, "thenTask");
    Objects.requireNonNull(otherwise, "otherwise");

    return list ->
        list.switchCase(sw -> sw.on(c -> c.when(jqExpression).then(thenTask)).onDefault(otherwise));
  }

  /**
   * JQ-based condition: if the JQ expression evaluates truthy → jump to {@code thenTask}, else jump
   * to {@code otherwiseTask}.
   *
   * <pre>
   *   switchWhenOrElse(".score >= 80", "pass", "fail")
   * </pre>
   *
   * <p>The JQ expression is evaluated against the task input at runtime.
   */
  public static FuncTaskConfigurer switchWhenOrElse(
      String jqExpression, String thenTask, String otherwiseTask) {

    Objects.requireNonNull(jqExpression, "jqExpression");
    Objects.requireNonNull(thenTask, "thenTask");
    Objects.requireNonNull(otherwiseTask, "otherwiseTask");

    return list ->
        list.switchCase(
            sw -> sw.on(c -> c.when(jqExpression).then(thenTask)).onDefault(otherwiseTask));
  }

  /**
   * Java functional {@code forEach}: collection is computed from the current input at runtime.
   *
   * @param collection function that returns the collection to iterate
   * @param body inner task list body
   * @param <T> input type for the collection function
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer forEach(
      Function<T, Collection<?>> collection, Consumer<FuncTaskItemListBuilder> body) {
    return list -> list.forEach(j -> j.collection(collection).tasks(body));
  }

  /**
   * Java functional {@code forEach}: iterate over a constant collection.
   *
   * @param collection static collection to iterate
   * @param body inner task list body
   * @param <T> ignored (kept for signature consistency)
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer forEach(
      Collection<?> collection, Consumer<FuncTaskItemListBuilder> body) {
    Function<T, Collection<?>> f = ctx -> (Collection<?>) collection;
    return list -> list.forEach(j -> j.collection(f).tasks(body));
  }

  /**
   * Java functional {@code forEach} helper for an immutable {@link List}.
   *
   * @param collection list to iterate
   * @param body inner task list body
   * @param <T> element type
   * @return list configurer
   */
  public static <T> FuncTaskConfigurer forEach(
      List<T> collection, Consumer<FuncTaskItemListBuilder> body) {
    return list -> list.forEach(j -> j.collection(ctx -> collection).tasks(body));
  }

  /**
   * Set a raw JQ-like expression on the current list (advanced).
   *
   * @param expr expression string
   * @return list configurer
   */
  public static FuncTaskConfigurer set(String expr) {
    return list -> list.set(expr);
  }

  /**
   * Set a map-based expression on the current list (advanced).
   *
   * @param map map of values to set
   * @return list configurer
   */
  public static FuncTaskConfigurer set(Map<String, Object> map) {
    return list -> list.set(s -> s.expr(map));
  }

  /**
   * Low-level HTTP call entrypoint using a {@link FuncCallHttpConfigurer}.
   *
   * <p>This overload creates an unnamed HTTP task.
   *
   * @param configurer the configurer that mutates the underlying HTTP call builder
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task to the tasks list
   */
  public static FuncTaskConfigurer call(FuncCallHttpConfigurer configurer) {
    return call(null, configurer);
  }

  /**
   * Low-level HTTP call entrypoint using a {@link FuncCallHttpConfigurer}.
   *
   * <p>This overload allows assigning an explicit task name.
   *
   * @param name task name, or {@code null} for an anonymous task
   * @param configurer the configurer that mutates the underlying HTTP call builder
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task to the tasks list
   */
  public static FuncTaskConfigurer call(String name, FuncCallHttpConfigurer configurer) {
    Objects.requireNonNull(configurer, "configurer");
    return list -> list.http(name, configurer);
  }

  /**
   * HTTP call using a fluent {@link FuncCallHttpSpec}.
   *
   * <p>This overload creates an unnamed HTTP task.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.call(
   *     FuncDSL.http()
   *       .GET()
   *       .endpoint("http://service/api")
   *   )
   * );
   * }</pre>
   *
   * @param spec fluent HTTP spec built via {@link #http()}
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task
   */
  public static FuncTaskConfigurer call(FuncCallHttpSpec spec) {
    return call(null, spec);
  }

  /**
   * HTTP call using a fluent {@link FuncCallHttpSpec} with explicit task name.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.call("fetchUsers",
   *     FuncDSL.http()
   *       .GET()
   *       .endpoint("http://service/users")
   *   )
   * );
   * }</pre>
   *
   * @param name task name, or {@code null} for an anonymous task
   * @param spec fluent HTTP spec built via {@link #http()}
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task
   */
  public static FuncTaskConfigurer call(String name, FuncCallHttpSpec spec) {
    Objects.requireNonNull(spec, "spec");
    return call(name, spec::accept);
  }

  /**
   * Create a new, empty HTTP specification to be used with {@link #call(FuncCallHttpSpec)}.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * FuncDSL.call(
   *   FuncDSL.http()
   *     .GET()
   *     .endpoint("http://service/api")
   *     .acceptJSON()
   * );
   * }</pre>
   *
   * @return a new {@link FuncCallHttpSpec}
   */
  public static FuncCallHttpSpec http() {
    return new FuncCallHttpSpec();
  }

  /**
   * Create a new HTTP specification preconfigured with an endpoint expression and authentication.
   *
   * <pre>{@code
   * FuncDSL.call(
   *   FuncDSL.http("http://service/api", auth -> auth.use("my-auth"))
   *     .GET()
   * );
   * }</pre>
   *
   * @param urlExpr expression or literal string for the endpoint URL
   * @param auth authentication configurer (e.g. {@code auth -> auth.use("my-auth")})
   * @return a {@link FuncCallHttpSpec} preconfigured with endpoint + auth
   */
  public static FuncCallHttpSpec http(String urlExpr, AuthenticationConfigurer auth) {
    return new FuncCallHttpSpec().endpoint(urlExpr, auth);
  }

  /**
   * Create a new HTTP specification preconfigured with a {@link URI} and authentication.
   *
   * @param url concrete URI to call
   * @param auth authentication configurer
   * @return a {@link FuncCallHttpSpec} preconfigured with URI + auth
   */
  public static FuncCallHttpSpec http(URI url, AuthenticationConfigurer auth) {
    return new FuncCallHttpSpec().uri(url, auth);
  }

  /**
   * Convenience for adding an unnamed {@code GET} HTTP task using a string endpoint.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.get("http://service/health")
   * );
   * }</pre>
   *
   * @param endpoint literal or expression for the endpoint URL
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(String endpoint) {
    return get(null, endpoint);
  }

  /**
   * Convenience for adding a named {@code GET} HTTP task using a string endpoint.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.get("checkHealth", "http://service/health")
   * );
   * }</pre>
   *
   * @param name task name
   * @param endpoint literal or expression for the endpoint URL
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(String name, String endpoint) {
    return call(name, http().GET().endpoint(endpoint));
  }

  /**
   * Convenience for adding an unnamed authenticated {@code GET} HTTP task using a string endpoint.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.get("http://service/api/users", auth -> auth.use("user-service-auth"))
   * );
   * }</pre>
   *
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(String endpoint, AuthenticationConfigurer auth) {
    return get(null, endpoint, auth);
  }

  /**
   * Convenience for adding a named authenticated {@code GET} HTTP task using a string endpoint.
   *
   * @param name task name
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(
      String name, String endpoint, AuthenticationConfigurer auth) {
    return call(name, http().GET().endpoint(endpoint, auth));
  }

  /**
   * Convenience for adding an unnamed {@code GET} HTTP task using a {@link URI}.
   *
   * @param endpoint concrete URI to call
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(URI endpoint) {
    return get(null, endpoint);
  }

  /**
   * Convenience for adding a named {@code GET} HTTP task using a {@link URI}.
   *
   * @param name task name
   * @param endpoint concrete URI to call
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(String name, URI endpoint) {
    return call(name, http().GET().uri(endpoint));
  }

  /**
   * Convenience for adding an unnamed authenticated {@code GET} HTTP task using a {@link URI}.
   *
   * @param endpoint concrete URI to call
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(URI endpoint, AuthenticationConfigurer auth) {
    return get(null, endpoint, auth);
  }

  /**
   * Convenience for adding a named authenticated {@code GET} HTTP task using a {@link URI}.
   *
   * @param name task name
   * @param endpoint concrete URI to call
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding a {@code GET} HTTP task
   */
  public static FuncTaskConfigurer get(String name, URI endpoint, AuthenticationConfigurer auth) {
    return call(name, http().GET().uri(endpoint, auth));
  }

  /**
   * Convenience for adding an unnamed {@code POST} HTTP task with a body and string endpoint.
   *
   * <pre>{@code
   * tasks(
   *   FuncDSL.post(
   *     Map.of("name", "Ricardo"),
   *     "http://service/api/users"
   *   )
   * );
   * }</pre>
   *
   * @param body HTTP request body (literal value or expression-compatible object)
   * @param endpoint literal or expression for the endpoint URL
   * @return a {@link FuncTaskConfigurer} adding a {@code POST} HTTP task
   */
  public static FuncTaskConfigurer post(Object body, String endpoint) {
    return post(null, body, endpoint);
  }

  /**
   * Convenience for adding a named {@code POST} HTTP task with a body and string endpoint.
   *
   * @param name task name
   * @param body HTTP request body (literal value or expression-compatible object)
   * @param endpoint literal or expression for the endpoint URL
   * @return a {@link FuncTaskConfigurer} adding a {@code POST} HTTP task
   */
  public static FuncTaskConfigurer post(String name, Object body, String endpoint) {
    return call(name, http().POST().endpoint(endpoint).body(body));
  }

  /**
   * Convenience for adding an unnamed authenticated {@code POST} HTTP task with body and endpoint.
   *
   * @param body HTTP request body
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding an authenticated {@code POST} HTTP task
   */
  public static FuncTaskConfigurer post(
      Object body, String endpoint, AuthenticationConfigurer auth) {
    return post(null, body, endpoint, auth);
  }

  /**
   * Convenience for adding a named authenticated {@code POST} HTTP task with body and endpoint.
   *
   * @param name task name
   * @param body HTTP request body
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncTaskConfigurer} adding an authenticated {@code POST} HTTP task
   */
  public static FuncTaskConfigurer post(
      String name, Object body, String endpoint, AuthenticationConfigurer auth) {

    return call(name, http().POST().endpoint(endpoint, auth).body(body));
  }
}
