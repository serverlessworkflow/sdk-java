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
import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.fluent.func.FuncCallTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncEmitTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncSwitchTaskBuilder;
import io.serverlessworkflow.fluent.func.FuncTaskItemListBuilder;
import io.serverlessworkflow.fluent.func.configurers.FuncCallHttpConfigurer;
import io.serverlessworkflow.fluent.func.configurers.FuncCallOpenAPIConfigurer;
import io.serverlessworkflow.fluent.func.configurers.FuncTaskConfigurer;
import io.serverlessworkflow.fluent.func.configurers.SwitchCaseConfigurer;
import io.serverlessworkflow.fluent.spec.AbstractEventConsumptionStrategyBuilder;
import io.serverlessworkflow.fluent.spec.EventFilterBuilder;
import io.serverlessworkflow.fluent.spec.ScheduleBuilder;
import io.serverlessworkflow.fluent.spec.TimeoutBuilder;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.fluent.spec.dsl.DSL;
import io.serverlessworkflow.fluent.spec.dsl.UseSpec;
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
 *   <li>Expose chainable steps (e.g., {@code emit(...).exportAs(...).when(...)}) via {@link Step}
 *       subclasses.
 *   <li>Provide opinionated helpers for CloudEvents (JSON/bytes) and listen strategies.
 *   <li>Offer context-aware function variants ({@code withContext}, {@code withInstanceId}, {@code
 *       withUniqueId}, {@code agent}).
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * Workflow wf = FuncWorkflowBuilder.workflow("example")
 * .tasks(
 * FuncDSL.function(String::trim, String.class),
 * FuncDSL.emit(FuncDSL.produced("org.acme.started").jsonData(MyPayload.class)),
 * FuncDSL.listen(FuncDSL.toAny("type.one", "type.two"))
 * .outputAs(map -> map.get("value")),
 * FuncDSL.switchWhenOrElse((Integer v) -> v > 0, "positive", FlowDirectiveEnum.END, Integer.class)
 * ).build();
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
  public static <T, V> Consumer<FuncCallTaskBuilder> fn(SerializableFunction<T, V> function) {
    return f -> f.function(function, ReflectionUtils.inferInputType(function));
  }

  /**
   * Compose multiple switch cases into a single configurer for {@link FuncSwitchTaskBuilder}.
   *
   * @param cases one or more {@link SwitchCaseConfigurer} built via {@link
   *     #caseOf(SerializablePredicate)} or {@link #caseDefault(String)}
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
  public static <T> SwitchCaseSpec<T> caseOf(SerializablePredicate<T> when) {
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
    return new FuncListenSpec().one(consumed(type));
  }

  public static FuncListenSpec toOne(FuncEventFilterSpec filter) {
    return new FuncListenSpec().one(filter);
  }

  /**
   * Convenience to listen for all the given event types (logical AND).
   *
   * @param types CloudEvent types
   * @return a {@link FuncListenSpec} set to {@code all(types...)}
   */
  public static FuncListenSpec toAll(String... types) {
    FuncEventFilterSpec[] events = new FuncEventFilterSpec[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = consumed(types[i]);
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
    FuncEventFilterSpec[] events = new FuncEventFilterSpec[types.length];
    for (int i = 0; i < types.length; i++) {
      events[i] = consumed(types[i]);
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
  public static <T> Consumer<FuncEmitTaskBuilder> produced(
      String type, SerializableFunction<T, CloudEventData> function) {
    return event ->
        event.event(e -> e.type(type).data(function, ReflectionUtils.inferInputType(function)));
  }

  /**
   * Same as {@link #produced(String, SerializableFunction)} but with an explicit input class to
   * guide conversion.
   *
   * @param type CloudEvent type
   * @param function function that maps workflow input to {@link CloudEventData}
   * @param inputClass expected input class for conversion
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> produced(
      String type, Function<T, CloudEventData> function, Class<T> inputClass) {
    return event -> event.event(e -> e.type(type).data(function, inputClass));
  }

  public static <T> Consumer<FuncEmitTaskBuilder> produced(
      String type, ContextFunction<T, CloudEventData> function) {
    return event ->
        event.event(e -> e.type(type).data(function, ReflectionUtils.inferInputType(function)));
  }

  public static <T> Consumer<FuncEmitTaskBuilder> produced(
      String type, FilterFunction<T, CloudEventData> function) {
    return event ->
        event.event(e -> e.type(type).data(function, ReflectionUtils.inferInputType(function)));
  }

  /**
   * Emit a JSON CloudEvent for a POJO input type. Sets {@code contentType=application/json} and
   * serializes the input to bytes using the configured JSON mapper.
   *
   * @param type CloudEvent type
   * @param inputClass input POJO class (used for typing and conversion)
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> producedJson(String type, Class<T> inputClass) {
    return b -> new FuncEmitSpec().type(type).jsonData(inputClass).accept(b);
  }

  /**
   * Emit a CloudEvent with arbitrary bytes payload generated by a custom serializer.
   *
   * @param type CloudEvent type
   * @param serializer function producing bytes from the input
   * @param inputClass expected input class for conversion
   * @param <T> input type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static <T> Consumer<FuncEmitTaskBuilder> producedBytes(
      String type, Function<T, byte[]> serializer, Class<T> inputClass) {
    return b -> new FuncEmitSpec().type(type).bytesData(serializer, inputClass).accept(b);
  }

  /**
   * Emit a CloudEvent where the input model is serialized to bytes using UTF-8. Useful for
   * string-like payloads (e.g., already-built JSON or text).
   *
   * @param type CloudEvent type
   * @return a consumer to configure {@link FuncEmitTaskBuilder}
   */
  public static Consumer<FuncEmitTaskBuilder> producedBytesUtf8(String type) {
    return b -> new FuncEmitSpec().type(type).bytesDataUtf8().accept(b);
  }

  /**
   * Starts building an event emission specification with a predefined type.
   *
   * @param type CloudEvent type to be emitted
   * @return a new {@link FuncEmitSpec} instance pre-configured with the event type
   */
  public static FuncEmitSpec produced(String type) {
    return new FuncEmitSpec().type(type);
  }

  /**
   * Starts building a function-centric event filter specification for a specific CloudEvent type. *
   *
   * <p>This creates an empty {@link FuncEventFilterSpec} which acts as a fluent builder for
   * matching incoming CloudEvents. It is typically passed to a {@code listen} strategy like {@link
   * #toOne(String)} or {@code to().any(...)}.
   *
   * @param type the {@code type} attribute of the CloudEvent to listen for
   * @return a new {@link FuncEventFilterSpec} instance pre-configured with the event type
   */
  public static FuncEventFilterSpec consumed(String type) {
    return new FuncEventFilterSpec().type(type);
  }

  /**
   * Build a call step for functions that need {@link WorkflowContextData} as the first parameter.
   * The DSL wraps it as a {@link ContextFunction} and injects the runtime context.
   *
   * <p>Signature expected: {@code (ctx, payload) -> result}
   *
   * @param fn context-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withContext(ContextFunction<T, R> fn, Class<T> in) {
    return withContext(null, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> withContext(ContextFunction<T, R> fn) {
    return withContext(null, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Named variant of {@link #withContext(ContextFunction, Class)}.
   *
   * @param name task name
   * @param fn context-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withContext(
      String name, ContextFunction<T, R> fn, Class<T> in) {
    return new FuncCallStep<>(name, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> withContext(String name, ContextFunction<T, R> fn) {
    return new FuncCallStep<>(name, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Build a call step for functions that need {@link WorkflowContextData} and {@link
   * TaskContextData} as the first and second parameter. The DSL wraps it as a {@link
   * FilterFunction} and injects the runtime context.
   *
   * <p>Signature expected: {@code (payload, wctx, tctx) -> result}
   *
   * @param fn context-aware filter function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withFilter(FilterFunction<T, R> fn, Class<T> in) {
    return withFilter(null, fn, in);
  }

  /**
   * Named variant of {@link #withFilter(FilterFunction, Class)}.
   *
   * @param name task name
   * @param fn context-aware filter function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withFilter(
      String name, FilterFunction<T, R> fn, Class<T> in) {
    return new FuncCallStep<>(name, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> withFilter(FilterFunction<T, R> fn) {
    return withFilter(null, fn, ReflectionUtils.inferInputType(fn));
  }

  public static <T, R> FuncCallStep<T, R> withFilter(String name, FilterFunction<T, R> fn) {
    return withFilter(name, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Named variant of {@link #withInstanceId(InstanceIdFunction, Class)}.
   *
   * @param name task name
   * @param fn instance-id-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> withInstanceId(
      String name, InstanceIdFunction<T, R> fn, Class<T> in) {
    ContextFunction<T, R> jcf = (payload, wctx) -> fn.apply(wctx.instanceData().id(), payload);
    return new FuncCallStep<>(name, jcf, in);
  }

  /**
   * Build a call step for functions that expect the workflow instance ID as the first parameter.
   * The instance ID is extracted from the runtime context.
   *
   * <p>Signature expected: {@code (instanceId, payload) -> result}
   *
   * @param fn instance-id-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withInstanceId(InstanceIdFunction<T, R> fn, Class<T> in) {
    return withInstanceId(null, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> withInstanceId(String name, InstanceIdFunction<T, R> fn) {
    return withInstanceId(name, fn, ReflectionUtils.inferInputType(fn));
  }

  public static <T, R> FuncCallStep<T, R> withInstanceId(InstanceIdFunction<T, R> fn) {
    return withInstanceId(null, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Builds a composition of the current workflow instance id and the definition of the task
   * position as a JSON pointer, used as a stable "unique id" for the task.
   *
   * @param wctx workflow context
   * @param tctx task context
   * @return a unique id in the form {@code "<instanceId>-<jsonPointer>"}
   */
  static String defaultUniqueId(WorkflowContextData wctx, TaskContextData tctx) {
    return String.format("%s-%s", wctx.instanceData().id(), tctx.position().jsonPointer());
  }

  /**
   * Build a call step for functions that expect a composite "unique id" as the first parameter.
   * This id is derived from the workflow instance id and the task definition position, encoded as a
   * JSON pointer.
   *
   * <p>Signature expected: {@code (uniqueId, payload) -> result}
   *
   * @param name task name (or {@code null} for an anonymous task)
   * @param fn unique-id-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withUniqueId(
      String name, UniqueIdBiFunction<T, R> fn, Class<T> in) {
    FilterFunction<T, R> jff =
        (payload, wctx, tctx) -> fn.apply(defaultUniqueId(wctx, tctx), payload);
    return new FuncCallStep<>(name, jff, in);
  }

  public static <T, R> FuncCallStep<T, R> withUniqueId(String name, UniqueIdBiFunction<T, R> fn) {
    return withUniqueId(name, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Variant of {@link #withUniqueId(String, UniqueIdBiFunction, Class)} without an explicit task
   * name.
   *
   * @param fn unique-id-aware function
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> withUniqueId(UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(null, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> withUniqueId(UniqueIdBiFunction<T, R> fn) {
    return withUniqueId(null, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Create a fire-and-forget side-effect step (unnamed). The consumer receives the typed input.
   *
   * @param consumer side-effect function
   * @param inputClass expected input class for conversion
   * @param <T> input type
   * @return a {@link ConsumeStep} which can be chained and added via {@link
   *     #tasks(FuncTaskConfigurer...)}
   */
  public static <T> ConsumeStep<T> consume(Consumer<T> consumer, Class<T> inputClass) {
    return new ConsumeStep<>(consumer, inputClass);
  }

  public static <T> ConsumeStep<T> consume(SerializableConsumer<T> consumer) {
    return consume(consumer, ReflectionUtils.inferInputType(consumer));
  }

  /**
   * Named variant of {@link #consume(Consumer, Class)}.
   *
   * @param name task name
   * @param consumer side-effect function
   * @param inputClass expected input class
   * @param <T> input type
   * @return a named {@link ConsumeStep}
   */
  public static <T> ConsumeStep<T> consume(String name, Consumer<T> consumer, Class<T> inputClass) {
    return new ConsumeStep<>(name, consumer, inputClass);
  }

  public static <T> ConsumeStep<T> consume(String name, SerializableConsumer<T> consumer) {
    return consume(name, consumer, ReflectionUtils.inferInputType(consumer));
  }

  /**
   * Agent-style sugar for methods that receive a "memory id" as first parameter. The DSL uses a
   * derived unique id composed of the workflow instance id and the task position (JSON pointer),
   * via {@link #withUniqueId(UniqueIdBiFunction, Class)}.
   *
   * <p>Signature expected: {@code (uniqueId, payload) -> result}
   *
   * @param fn (uniqueId, payload) -> result
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a call step
   */
  public static <T, R> FuncCallStep<T, R> agent(UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(fn, in);
  }

  public static <T, R> FuncCallStep<T, R> agent(UniqueIdBiFunction<T, R> fn) {
    return withUniqueId(fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Named agent-style sugar. See {@link #agent(UniqueIdBiFunction, Class)}.
   *
   * <p>Signature expected: {@code (uniqueId, payload) -> result}
   *
   * @param name task name
   * @param fn (uniqueId, payload) -> result
   * @param in payload input class
   * @param <T> input type
   * @param <R> result type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> agent(
      String name, UniqueIdBiFunction<T, R> fn, Class<T> in) {
    return withUniqueId(name, fn, in);
  }

  public static <T, R> FuncCallStep<T, R> agent(String name, UniqueIdBiFunction<T, R> fn) {
    return withUniqueId(name, fn, ReflectionUtils.inferInputType(fn));
  }

  /**
   * Create a {@link FuncCallStep} that calls a simple Java {@link Function} with explicit input
   * type.
   *
   * @param fn the function to execute at runtime
   * @param inputClass expected input class for model conversion
   * @param <T> input type
   * @param <R> result type
   * @return a call step which supports chaining (e.g., {@code .exportAs(...).when(...)})
   */
  public static <T, R> FuncCallStep<T, R> function(Function<T, R> fn, Class<T> inputClass) {
    return new FuncCallStep<>(fn, inputClass);
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
  public static <T, R> FuncCallStep<T, R> function(SerializableFunction<T, R> fn) {
    Class<T> inputClass = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(fn, inputClass);
  }

  /**
   * Named variant of {@link #function(SerializableFunction)} with inferred input type.
   *
   * @param name task name
   * @param fn the function to execute
   * @param <T> input type
   * @param <R> output type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> function(String name, SerializableFunction<T, R> fn) {
    Class<T> inputClass = ReflectionUtils.inferInputType(fn);
    return new FuncCallStep<>(name, fn, inputClass);
  }

  /**
   * Named variant of {@link #function(Function, Class)} with explicit input type.
   *
   * @param name task name
   * @param fn the function to execute
   * @param inputClass expected input class
   * @param <T> input type
   * @param <R> output type
   * @return a named call step
   */
  public static <T, R> FuncCallStep<T, R> function(
      String name, Function<T, R> fn, Class<T> inputClass) {
    return new FuncCallStep<>(name, fn, inputClass);
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
   * Starts building a function-centric event emission specification.
   *
   * <p>This creates an empty {@link FuncEmitSpec} which acts as a fluent builder for the properties
   * (e.g., type, source, data) of the CloudEvent to be emitted. It is typically passed to the
   * {@link #emit(Consumer)} step.
   *
   * @return a new {@link FuncEmitSpec} instance for fluent configuration
   */
  public static FuncEmitSpec produced() {
    return new FuncEmitSpec();
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
   * @return a named {@link EmitStep}
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
  public static <T> EmitStep emit(String type, SerializableFunction<T, CloudEventData> fn) {
    return new EmitStep(null, produced(type, fn));
  }

  /**
   * Named variant of {@link #emit(String, SerializableFunction)}.
   *
   * @param name task name
   * @param type CloudEvent type
   * @param fn function producing {@link CloudEventData}
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emit(
      String name, String type, SerializableFunction<T, CloudEventData> fn) {
    return new EmitStep(name, produced(type, fn));
  }

  /**
   * Emit a bytes-based CloudEvent using a custom serializer and explicit input class.
   *
   * @param name task name
   * @param type CloudEvent type
   * @param serializer function producing bytes
   * @param inputClass expected input class
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emit(
      String name, String type, Function<T, byte[]> serializer, Class<T> inputClass) {
    return new EmitStep(name, producedBytes(type, serializer, inputClass));
  }

  /**
   * Unnamed variant of {@link #emit(String, String, Function, Class)}.
   *
   * @param type CloudEvent type
   * @param serializer function producing bytes
   * @param inputClass expected input class
   * @param <T> input type
   * @return an {@link EmitStep}
   */
  public static <T> EmitStep emit(
      String type, Function<T, byte[]> serializer, Class<T> inputClass) {
    return new EmitStep(null, producedBytes(type, serializer, inputClass));
  }

  /**
   * Emit a JSON CloudEvent from a POJO input class (unnamed).
   *
   * @param type CloudEvent type
   * @param inputClass input POJO class
   * @param <T> input type
   * @return an {@link EmitStep}
   */
  public static <T> EmitStep emitJson(String type, Class<T> inputClass) {
    return new EmitStep(null, producedJson(type, inputClass));
  }

  /**
   * Emit a JSON CloudEvent from a POJO input class (named).
   *
   * @param name task name
   * @param type CloudEvent type
   * @param inputClass input POJO class
   * @param <T> input type
   * @return a named {@link EmitStep}
   */
  public static <T> EmitStep emitJson(String name, String type, Class<T> inputClass) {
    return new EmitStep(name, producedJson(type, inputClass));
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
   * #caseOf(SerializablePredicate)} helpers when possible.
   *
   * @param taskName optional task name
   * @param switchCase consumer to configure the {@link FuncSwitchTaskBuilder}
   * @return a list configurer
   */
  public static FuncTaskConfigurer switchCase(
      String taskName, Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(taskName, switchCase);
  }

  /**
   * Variant of {@link #switchCase(String, Consumer)} without a name.
   *
   * @param switchCase consumer to configure the {@link FuncSwitchTaskBuilder}
   * @return a list configurer
   */
  public static FuncTaskConfigurer switchCase(Consumer<FuncSwitchTaskBuilder> switchCase) {
    return list -> list.switchCase(switchCase);
  }

  /**
   * Convenience to apply multiple {@link SwitchCaseConfigurer} built via {@link
   * #caseOf(SerializablePredicate)}.
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
   * switchWhen(".approved == true", "approveOrder")
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

  public static <T> FuncTaskConfigurer switchWhenOrElse(
      SerializablePredicate<T> pred, String thenTask, FlowDirectiveEnum otherwise) {
    return switchWhenOrElse(pred, thenTask, otherwise, ReflectionUtils.inferInputType(pred));
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

  public static <T> FuncTaskConfigurer switchWhenOrElse(
      SerializablePredicate<T> pred, String thenTask, String otherwiseTask) {
    return switchWhenOrElse(pred, thenTask, otherwiseTask, ReflectionUtils.inferInputType(pred));
  }

  /**
   * JQ-based condition: if the JQ expression evaluates truthy → jump to {@code thenTask}, otherwise
   * follow the {@link FlowDirectiveEnum} given in {@code otherwise}.
   *
   * <pre>
   * switchWhenOrElse(".approved == true", "sendEmail", FlowDirectiveEnum.END)
   * </pre>
   *
   * <p>The JQ expression is evaluated against the task input at runtime.
   *
   * @param jqExpression JQ expression evaluated against the current task input
   * @param thenTask task to jump to if the expression evaluates truthy
   * @param otherwise default flow directive when the expression is falsy
   * @return list configurer
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
   * switchWhenOrElse(".score >= 80", "pass", "fail")
   * </pre>
   *
   * <p>The JQ expression is evaluated against the task input at runtime.
   *
   * @param jqExpression JQ expression evaluated against the current task input
   * @param thenTask task name when truthy
   * @param otherwiseTask task name when falsy
   * @return list configurer
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
   * Set a raw expression on the current list (advanced).
   *
   * @param expr expression string
   * @return list configurer
   */
  public static FuncTaskConfigurer set(String expr) {
    return list -> list.set(expr);
  }

  /**
   * Set values on the current list from a map (advanced).
   *
   * @param map map of values to set
   * @return list configurer
   */
  public static FuncTaskConfigurer set(Map<String, Object> map) {
    return list -> list.set(s -> s.expr(map));
  }

  // ---------------------------------------------------------------------------
  // HTTP / OpenAPI
  // ---------------------------------------------------------------------------

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
   * HTTP call using a fluent {@link FuncCallHttpStep}.
   *
   * <p>This overload creates an unnamed HTTP task.
   *
   * <pre>{@code
   * tasks(FuncDSL.call(FuncDSL.http().GET().endpoint("http://service/api")));
   * }</pre>
   *
   * @param spec fluent HTTP spec built via {@link #http()}
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task
   */
  public static FuncTaskConfigurer call(FuncCallHttpStep spec) {
    return call(null, spec);
  }

  /**
   * HTTP call using a fluent {@link FuncCallHttpStep} with explicit task name.
   *
   * <pre>{@code
   * tasks(FuncDSL.call("fetchUsers", FuncDSL.http().GET().endpoint("http://service/users")));
   * }</pre>
   *
   * @param name task name, or {@code null} for an anonymous task
   * @param spec fluent HTTP spec built via {@link #http()}
   * @return a {@link FuncTaskConfigurer} that adds an HTTP task
   */
  public static FuncTaskConfigurer call(String name, FuncCallHttpStep spec) {
    Objects.requireNonNull(spec, "spec");
    return call(name, (FuncCallHttpConfigurer) spec::accept);
  }

  /**
   * OpenAPI call using a fluent {@link FuncCallOpenAPIStep}.
   *
   * <p>This overload creates an unnamed OpenAPI call task.
   *
   * <pre>{@code
   * FuncWorkflowBuilder.workflow("openapi-call")
   *   .tasks(call(openapi().document("https://petstore.swagger.io/v2/swagger.json", auth("openapi-auth")).operation("getPetById"))
   * )
   * .build();
   * }</pre>
   *
   * @param spec fluent OpenAPI spec built via {@link #openapi()}
   * @return a {@link FuncTaskConfigurer} that adds an OpenAPI call task to the workflow
   */
  public static FuncTaskConfigurer call(FuncCallOpenAPIStep spec) {
    return call(null, spec);
  }

  /**
   * OpenAPI call using a fluent {@link FuncCallOpenAPIStep} with an explicit task name.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FuncWorkflowBuilder.workflow("openapi-call-named")
   * .tasks(
   * FuncDSL.call(
   * "fetchPet",
   * FuncDSL.openapi()
   * .document("https://petstore.swagger.io/v2/swagger.json", DSL.auth("openapi-auth"))
   * .operation("getPetById")
   * .parameter("id", 123)
   * )
   * )
   * .build();
   * }</pre>
   *
   * @param name task name, or {@code null} for an anonymous task
   * @param spec fluent OpenAPI spec built via {@link #openapi()}
   * @return a {@link FuncTaskConfigurer} that adds a named OpenAPI call task
   */
  public static FuncTaskConfigurer call(String name, FuncCallOpenAPIStep spec) {
    Objects.requireNonNull(spec, "spec");
    spec.setName(name);
    return spec;
  }

  /**
   * Low-level OpenAPI call entrypoint using a {@link FuncCallOpenAPIConfigurer}.
   *
   * <p>This overload creates an unnamed OpenAPI call task.
   *
   * @param configurer configurer that mutates the underlying OpenAPI call builder
   * @return a {@link FuncTaskConfigurer} that adds an OpenAPI call task
   */
  public static FuncTaskConfigurer call(FuncCallOpenAPIConfigurer configurer) {
    return call(null, configurer);
  }

  /**
   * Low-level OpenAPI call entrypoint using a {@link FuncCallOpenAPIConfigurer}.
   *
   * <p>This overload allows assigning an explicit task name.
   *
   * @param name task name, or {@code null} for an anonymous task
   * @param configurer configurer that mutates the underlying OpenAPI call builder
   * @return a {@link FuncTaskConfigurer} that adds an OpenAPI call task
   */
  public static FuncTaskConfigurer call(String name, FuncCallOpenAPIConfigurer configurer) {
    Objects.requireNonNull(configurer, "configurer");
    return list -> list.openapi(name, configurer);
  }

  /**
   * Create a new OpenAPI specification to be used with {@link #call(FuncCallOpenAPIStep)}.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * FuncDSL.call(openapi().document("https://petstore.swagger.io/v2/swagger.json", DSL.auth("openapi-auth")).operation("getPetById").parameter("id", 123));
   * }</pre>
   *
   * <p>The returned spec is a fluent builder that records operations (document, operation,
   * parameters, authentication, etc.) and applies them to the underlying OpenAPI call task at build
   * time.
   *
   * @return a new {@link FuncCallOpenAPIStep}
   */
  public static FuncCallOpenAPIStep openapi() {
    return new FuncCallOpenAPIStep();
  }

  /**
   * Named variant of {@link #openapi()}.
   *
   * @param name task name to be used when the spec is attached via {@link
   *     #call(FuncCallOpenAPIStep)}
   * @return a new named {@link FuncCallOpenAPIStep}
   */
  public static FuncCallOpenAPIStep openapi(String name) {
    return new FuncCallOpenAPIStep(name);
  }

  /**
   * Create a new, empty HTTP specification to be used with {@link #call(FuncCallHttpStep)}.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * FuncDSL.call(http().GET().endpoint("http://service/api").acceptJSON());
   * }</pre>
   *
   * @return a new {@link FuncCallHttpStep}
   */
  public static FuncCallHttpStep http() {
    return new FuncCallHttpStep();
  }

  /**
   * Named variant of {@link #http()}.
   *
   * @param name task name to be used when the spec is attached via {@link #call(FuncCallHttpStep)}
   * @return a new named {@link FuncCallHttpStep}
   */
  public static FuncCallHttpStep http(String name) {
    return new FuncCallHttpStep(name);
  }

  /**
   * Create a new HTTP specification preconfigured with an endpoint expression and authentication.
   *
   * <pre>{@code
   * FuncDSL.call(
   * FuncDSL.http("http://service/api", auth -> auth.use("my-auth"))
   * .GET()
   * );
   * }</pre>
   *
   * @param urlExpr expression or literal string for the endpoint URL
   * @param auth authentication configurer (e.g. {@code auth -> auth.use("my-auth")})
   * @return a {@link FuncCallHttpStep} preconfigured with endpoint + auth
   */
  public static FuncCallHttpStep http(String urlExpr, AuthenticationConfigurer auth) {
    return new FuncCallHttpStep().endpoint(urlExpr, auth);
  }

  /**
   * Create a new HTTP specification preconfigured with a {@link URI} and authentication.
   *
   * @param url concrete URI to call
   * @param auth authentication configurer
   * @return a {@link FuncCallHttpStep} preconfigured with URI + auth
   */
  public static FuncCallHttpStep http(URI url, AuthenticationConfigurer auth) {
    return new FuncCallHttpStep().uri(url, auth);
  }

  /**
   * Convenience for building an unnamed {@code GET} HTTP step using a string endpoint.
   *
   * <pre>{@code
   * tasks(
   * FuncDSL.call(
   * FuncDSL.get("http://service/health")
   * )
   * );
   * }</pre>
   *
   * @param endpoint literal or expression for the endpoint URL
   * @return a {@link FuncCallHttpStep} that can be chained and later passed to {@link
   *     #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String endpoint) {
    return get(null, endpoint);
  }

  /**
   * Convenience for building a named {@code GET} HTTP step using a string endpoint.
   *
   * <pre>{@code
   * tasks(
   * FuncDSL.call(
   * FuncDSL.get("checkHealth", "http://service/health")
   * )
   * );
   * }</pre>
   *
   * @param name task name
   * @param endpoint literal or expression for the endpoint URL
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String name, String endpoint) {
    return http(name).GET().endpoint(endpoint);
  }

  /**
   * Convenience for building an unnamed authenticated {@code GET} HTTP step using a string
   * endpoint.
   *
   * <pre>{@code
   * tasks(
   * FuncDSL.call(
   * FuncDSL.get("http://service/api/users", auth -> auth.use("user-service-auth"))
   * )
   * );
   * }</pre>
   *
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String endpoint, AuthenticationConfigurer auth) {
    return get(null, endpoint, auth);
  }

  /**
   * Convenience for building a named authenticated {@code GET} HTTP step using a string endpoint.
   *
   * @param name task name
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String name, String endpoint, AuthenticationConfigurer auth) {
    return http(name).GET().endpoint(endpoint, auth);
  }

  /**
   * Convenience for building an unnamed {@code GET} HTTP step using a {@link URI}.
   *
   * @param endpoint concrete URI to call
   * @return a {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(URI endpoint) {
    return get(null, endpoint);
  }

  /**
   * Convenience for building a named {@code GET} HTTP step using a {@link URI}.
   *
   * @param name task name
   * @param endpoint concrete URI to call
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String name, URI endpoint) {
    return http(name).GET().uri(endpoint);
  }

  /**
   * Convenience for building an unnamed authenticated {@code GET} HTTP step using a {@link URI}.
   *
   * @param endpoint concrete URI to call
   * @param auth authentication configurer
   * @return a {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(URI endpoint, AuthenticationConfigurer auth) {
    return get(null, endpoint, auth);
  }

  /**
   * Convenience for building a named authenticated {@code GET} HTTP step using a {@link URI}.
   *
   * @param name task name
   * @param endpoint concrete URI to call
   * @param auth authentication configurer
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep get(String name, URI endpoint, AuthenticationConfigurer auth) {
    return http(name).GET().uri(endpoint, auth);
  }

  /**
   * Convenience for building an unnamed {@code POST} HTTP step with a body and string endpoint.
   *
   * <pre>{@code
   * tasks(
   * FuncDSL.call(
   * FuncDSL.post(
   * Map.of("name", "Ricardo"),
   * "http://service/api/users"
   * )
   * )
   * );
   * }</pre>
   *
   * @param body HTTP request body (literal value or expression-compatible object)
   * @param endpointExpr literal or expression for the endpoint URL
   * @return a {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep post(Object body, String endpointExpr) {
    return post(null, body, endpointExpr);
  }

  /**
   * Convenience for building a named {@code POST} HTTP step with a body and string endpoint.
   *
   * @param name task name
   * @param body HTTP request body (literal value or expression-compatible object)
   * @param endpoint literal or expression for the endpoint URL
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep post(String name, Object body, String endpoint) {
    return http(name).POST().endpoint(endpoint).body(body);
  }

  /**
   * Convenience for building an unnamed authenticated {@code POST} HTTP step with body and
   * endpoint.
   *
   * @param body HTTP request body
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep post(Object body, String endpoint, AuthenticationConfigurer auth) {
    return post(null, body, endpoint, auth);
  }

  /**
   * Convenience for building a named authenticated {@code POST} HTTP step with body and endpoint.
   *
   * @param name task name
   * @param body HTTP request body
   * @param endpoint literal or expression for the endpoint URL
   * @param auth authentication configurer
   * @return a named {@link FuncCallHttpStep} that can be passed to {@link #call(FuncCallHttpStep)}
   */
  public static FuncCallHttpStep post(
      String name, Object body, String endpoint, AuthenticationConfigurer auth) {

    return http(name).POST().endpoint(endpoint, auth).body(body);
  }

  /**
   * Extracts and deserializes the workflow input data into the specified type from a workflow
   * context.
   *
   * <p>This utility method provides type-safe access to the workflow's initial input.
   *
   * <p>Use this method when you have access to the {@link WorkflowContextData} and need to retrieve
   * the original input that was provided when the workflow instance was started.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * inputFrom((object, WorkflowContextData workflowContext) -> {
   *   OrderRequest order = input(workflowContext, OrderRequest.class);
   *   return new Input(order);
   * });
   * }</pre>
   *
   * @param <T> the type to deserialize the input into
   * @param context the workflow context containing instance data and input
   * @param inputClass the class object representing the target type for deserialization
   * @return the deserialized workflow input object of type T
   */
  public static <T> T input(WorkflowContextData context, Class<T> inputClass) {
    return context
        .instanceData()
        .input()
        .as(inputClass)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Workflow input is missing or cannot be deserialized into type "
                        + inputClass.getName()
                        + " when calling FuncDSL.input(WorkflowContextData, Class<T>)."));
  }

  /**
   * Extracts and deserializes the task input data into the specified type from a task context.
   *
   * <p>This utility method provides type-safe access to a task's input.
   *
   * <p>Use this method when you have access to the {@link TaskContextData} and need to retrieve the
   * input provided to that task.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * inputFrom((Object obj, TaskContextData taskContextData) -> {
   * OrderRequest order = input(taskContextData, OrderRequest.class);
   * return order;
   * });
   * }</pre>
   *
   * @param <T> the type to deserialize the input into
   * @param taskContextData the task context from which to retrieve the task input
   * @param inputClass the class object representing the target type for deserialization
   * @return the deserialized task input object of type T
   */
  public static <T> T input(TaskContextData taskContextData, Class<T> inputClass) {
    return taskContextData
        .input()
        .as(inputClass)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Workflow input is missing or cannot be deserialized into type "
                        + inputClass.getName()
                        + " when calling FuncDSL.input(TaskContextData, Class<T>)."));
  }

  /**
   * Extracts and deserializes the output data from a task into the specified type.
   *
   * <p>This utility method provides type-safe access to a task's output.
   *
   * <p>Use this method when you need to access the result/output produced by a task execution. This
   * is particularly useful in subsequent tasks that need to process or transform the output of a
   * previous task in the workflow.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * .exportAs((object, workflowContext, taskContextData) -> {
   * Long output = output(taskContextData, Long.class);
   * return output * 2;
   * })
   * }</pre>
   *
   * @param <T> the type to deserialize the task output into
   * @param taskContextData the task context containing the output data
   * @param outputClass the class object representing the target type for deserialization
   * @return the deserialized task output object of type T
   */
  public static <T> T output(TaskContextData taskContextData, Class<T> outputClass) {
    return taskContextData
        .output()
        .as(outputClass)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Task output is missing or cannot be deserialized into type "
                        + outputClass.getName()
                        + " when calling FuncDSL.output(TaskContextData, Class<T>)."));
  }

  // ---------------------------------------------------------------------------
  // Facades to base DSL (Timeouts, Schedules, Event Strategies)
  // ---------------------------------------------------------------------------

  /**
   * Shortcut to configure a timeout in days.
   *
   * @see DSL#timeoutDays(int)
   */
  public static Consumer<TimeoutBuilder> timeoutDays(int days) {
    return DSL.timeoutDays(days);
  }

  /**
   * Shortcut to configure a timeout in hours.
   *
   * @see DSL#timeoutHours(int)
   */
  public static Consumer<TimeoutBuilder> timeoutHours(int hours) {
    return DSL.timeoutHours(hours);
  }

  /**
   * Shortcut to configure a timeout in minutes.
   *
   * @see DSL#timeoutMinutes(int)
   */
  public static Consumer<TimeoutBuilder> timeoutMinutes(int minutes) {
    return DSL.timeoutMinutes(minutes);
  }

  /**
   * Shortcut to configure a timeout in seconds.
   *
   * @see DSL#timeoutSeconds(int)
   */
  public static Consumer<TimeoutBuilder> timeoutSeconds(int seconds) {
    return DSL.timeoutSeconds(seconds);
  }

  /**
   * Shortcut to configure a timeout in milliseconds.
   *
   * @see DSL#timeoutMillis(int)
   */
  public static Consumer<TimeoutBuilder> timeoutMillis(int milliseconds) {
    return DSL.timeoutMillis(milliseconds);
  }

  // ---- Schedules ----//

  /**
   * @see DSL#every(Consumer)
   */
  public static Consumer<ScheduleBuilder> every(Consumer<TimeoutBuilder> duration) {
    return DSL.every(duration);
  }

  /**
   * @see DSL#every(String)
   */
  public static Consumer<ScheduleBuilder> every(String durationExpression) {
    return DSL.every(durationExpression);
  }

  /**
   * @see DSL#cron(String)
   */
  public static Consumer<ScheduleBuilder> cron(String cron) {
    return DSL.cron(cron);
  }

  /**
   * @see DSL#after(Consumer)
   */
  public static Consumer<ScheduleBuilder> after(Consumer<TimeoutBuilder> duration) {
    return DSL.after(duration);
  }

  /**
   * @see DSL#after(String)
   */
  public static Consumer<ScheduleBuilder> after(String durationExpression) {
    return DSL.after(durationExpression);
  }

  /**
   * @see DSL#on(Consumer)
   */
  public static Consumer<ScheduleBuilder> on(
      Consumer<AbstractEventConsumptionStrategyBuilder<?, ?, ?>> strategy) {
    return DSL.on(strategy);
  }

  // ---- Schedule Event Strategies ----//

  /**
   * @see DSL#one(String)
   */
  public static Consumer<AbstractEventConsumptionStrategyBuilder<?, ?, ?>> one(String eventType) {
    return DSL.one(eventType);
  }

  /**
   * @see DSL#one(Consumer)
   */
  public static Consumer<AbstractEventConsumptionStrategyBuilder<?, ?, ?>> one(
      Consumer<EventFilterBuilder> filter) {
    return DSL.one(filter);
  }

  /**
   * @see DSL#all(Consumer[])
   */
  @SafeVarargs
  public static Consumer<AbstractEventConsumptionStrategyBuilder<?, ?, ?>> all(
      Consumer<EventFilterBuilder>... filters) {
    return DSL.all(filters);
  }

  /**
   * @see DSL#any(Consumer[])
   */
  @SafeVarargs
  public static Consumer<AbstractEventConsumptionStrategyBuilder<?, ?, ?>> any(
      Consumer<EventFilterBuilder>... filters) {
    return DSL.any(filters);
  }

  // ---------------------------------------------------------------------------
  // Facades to base DSL (Use, Secrets, and Authentication)
  // ---------------------------------------------------------------------------

  /**
   * @see DSL#secret(String)
   */
  public static UseSpec secret(String secret) {
    return DSL.secret(secret);
  }

  /**
   * @see DSL#secrets(String...)
   */
  public static UseSpec secrets(String... secret) {
    return DSL.secrets(secret);
  }

  /**
   * @see DSL#auth(String, AuthenticationConfigurer)
   */
  public static UseSpec auth(String name, AuthenticationConfigurer auth) {
    return DSL.auth(name, auth);
  }

  /**
   * @see DSL#use()
   */
  public static UseSpec use() {
    return DSL.use();
  }

  /**
   * @see DSL#use(String)
   */
  public static AuthenticationConfigurer use(String authName) {
    return DSL.use(authName);
  }

  /**
   * @see DSL#basic(String, String)
   */
  public static AuthenticationConfigurer basic(String username, String password) {
    return DSL.basic(username, password);
  }

  /**
   * @see DSL#basic(String)
   */
  public static AuthenticationConfigurer basic(String secret) {
    return DSL.basic(secret);
  }

  /**
   * @see DSL#bearer(String)
   */
  public static AuthenticationConfigurer bearer(String token) {
    return DSL.bearer(token);
  }

  /**
   * @see DSL#bearerUse(String)
   */
  public static AuthenticationConfigurer bearerUse(String secret) {
    return DSL.bearerUse(secret);
  }

  /**
   * @see DSL#digest(String, String)
   */
  public static AuthenticationConfigurer digest(String username, String password) {
    return DSL.digest(username, password);
  }

  /**
   * @see DSL#digest(String)
   */
  public static AuthenticationConfigurer digest(String secret) {
    return DSL.digest(secret);
  }

  /**
   * @see DSL#oidc(String, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant)
   */
  public static AuthenticationConfigurer oidc(
      String authority, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant) {
    return DSL.oidc(authority, grant);
  }

  /**
   * @see DSL#oidc(String, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant, String, String)
   */
  public static AuthenticationConfigurer oidc(
      String authority,
      OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant,
      String clientId,
      String clientSecret) {
    return DSL.oidc(authority, grant, clientId, clientSecret);
  }

  /**
   * @see DSL#oidc(String)
   */
  public static AuthenticationConfigurer oidc(String secret) {
    return DSL.oidc(secret);
  }

  /**
   * @see DSL#oauth2(String,
   *     io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant)
   */
  public static AuthenticationConfigurer oauth2(
      String authority,
      io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant
          grant) {
    return DSL.oauth2(authority, grant);
  }

  /**
   * @see DSL#oauth2(String,
   *     io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant,
   *     String, String)
   */
  public static AuthenticationConfigurer oauth2(
      String authority,
      io.serverlessworkflow.api.types.OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant,
      String clientId,
      String clientSecret) {
    return DSL.oauth2(authority, grant, clientId, clientSecret);
  }

  /**
   * @see DSL#oauth2(String)
   */
  public static AuthenticationConfigurer oauth2(String secret) {
    return DSL.oauth2(secret);
  }
}
