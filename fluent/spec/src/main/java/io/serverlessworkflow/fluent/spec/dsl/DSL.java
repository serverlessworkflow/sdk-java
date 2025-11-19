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
package io.serverlessworkflow.fluent.spec.dsl;

import io.serverlessworkflow.api.types.OAuth2AuthenticationData;
import io.serverlessworkflow.fluent.spec.DoTaskBuilder;
import io.serverlessworkflow.fluent.spec.EmitTaskBuilder;
import io.serverlessworkflow.fluent.spec.ForkTaskBuilder;
import io.serverlessworkflow.fluent.spec.TaskItemListBuilder;
import io.serverlessworkflow.fluent.spec.TryTaskBuilder;
import io.serverlessworkflow.fluent.spec.configurers.AuthenticationConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.CallHttpConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.CallOpenAPIConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.ForEachConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.ListenConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.RaiseConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.RetryConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.SetConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.SwitchConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.TasksConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.TryCatchConfigurer;
import io.serverlessworkflow.fluent.spec.configurers.TryConfigurer;
import io.serverlessworkflow.types.Errors;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * High-level Java DSL shortcuts for building workflows.
 *
 * <p>This class exposes small, composable helpers that:
 *
 * <ul>
 *   <li>Build HTTP and OpenAPI call specs.
 *   <li>Configure {@code use} blocks (secrets and authentications).
 *   <li>Define switch, listen, event, try/catch and raise recipes.
 *   <li>Compose tasks lists and common task patterns.
 * </ul>
 *
 * <p>All methods are static and designed to be used with static imports.
 */
public final class DSL {

  private DSL() {}

  // ---- Convenient shortcuts ----//

  /**
   * Create a new HTTP call specification to be used with {@link #call(CallHttpConfigurer)} or
   * {@link #call(io.serverlessworkflow.fluent.func.dsl.FuncCallHttpSpec)}.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * tasks(
   *   call(
   *     http()
   *       .GET()
   *       .endpoint("http://service/api")
   *   )
   * );
   * }</pre>
   *
   * @return a new {@link CallHttpSpec} instance
   */
  public static CallHttpSpec http() {
    return new CallHttpSpec();
  }

  /**
   * Create a new OpenAPI call specification to be used with {@link #call(CallOpenAPIConfigurer)}.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * tasks(
   *   call(
   *     openapi()
   *       .document("http://service/openapi.json")
   *       .operation("getUser")
   *   )
   * );
   * }</pre>
   *
   * @return a new {@link CallOpenAPISpec} instance
   */
  public static CallOpenAPISpec openapi() {
    return new CallOpenAPISpec();
  }

  /**
   * Convenience for defining a {@code use} block with a single secret.
   *
   * <p>Example:
   *
   * <pre>{@code
   * workflow()
   *   .use(secret("db-password"))
   *   .build();
   * }</pre>
   *
   * @param secret secret identifier to add to the workflow
   * @return a {@link UseSpec} preconfigured with the given secret
   */
  public static UseSpec secret(String secret) {
    return new UseSpec().secret(secret);
  }

  /**
   * Convenience for defining a {@code use} block with multiple secrets.
   *
   * <p>Example:
   *
   * <pre>{@code
   * workflow()
   *   .use(secrets("db-password", "api-key"))
   *   .build();
   * }</pre>
   *
   * @param secret one or more secret identifiers
   * @return a {@link UseSpec} preconfigured with the given secrets
   */
  public static UseSpec secrets(String... secret) {
    return new UseSpec().secrets(secret);
  }

  /**
   * Convenience for defining a single reusable authentication policy by name.
   *
   * <p>Example:
   *
   * <pre>{@code
   * workflow()
   *   .use(auth("basic-auth", basic("user", "pass")));
   * }</pre>
   *
   * @param name logical authentication policy name
   * @param auth authentication configurer (e.g. {@link #basic(String, String)})
   * @return a {@link UseSpec} preconfigured with the given authentication
   */
  public static UseSpec auth(String name, AuthenticationConfigurer auth) {
    return new UseSpec().auth(name, auth);
  }

  /**
   * Create an empty {@link UseSpec} for incremental configuration.
   *
   * <p>Example:
   *
   * <pre>{@code
   * workflow()
   *   .use(
   *     use()
   *       .secret("db-password")
   *       .auth("basic-auth", basic("u", "p"))
   *   );
   * }</pre>
   *
   * @return a new, empty {@link UseSpec}
   */
  public static UseSpec use() {
    return new UseSpec();
  }

  /**
   * Create an empty {@link SwitchSpec} for building switch cases.
   *
   * <p>Typical usage is via chaining methods on {@link SwitchSpec} and passing it to {@link
   * #switchCase(SwitchConfigurer)}.
   *
   * @return a new {@link SwitchSpec}
   */
  public static SwitchSpec cases() {
    return new SwitchSpec();
  }

  /**
   * Start building a {@code listen} specification without a predefined strategy.
   *
   * <p>Use methods on {@link ListenSpec} like {@code one()}, {@code any()} or {@code all()} and
   * then pass the spec to {@link #listen(ListenConfigurer)}.
   *
   * @return a new {@link ListenSpec}
   */
  public static ListenSpec to() {
    return new ListenSpec();
  }

  /**
   * Start building an event emission specification.
   *
   * <p>Use methods on {@link EventSpec} to define event type and payload, and pass it to {@link
   * #emit(Consumer)}.
   *
   * @return a new {@link EventSpec}
   */
  public static EventSpec event() {
    return new EventSpec();
  }

  /**
   * Start building a {@code try/catch} specification for use with {@link #tryCatch(TryConfigurer)}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * tasks(
   *   tryCatch(
   *     tryCatch()
   *       .doTasks(...)
   *       .on(catchWhen("jqExpr", retryWhen(...), tasks(...)))
   *   )
   * );
   * }</pre>
   *
   * @return a new {@link TrySpec}
   */
  public static TrySpec tryCatch() {
    return new TrySpec();
  }

  /**
   * Build a {@link TryCatchConfigurer} that catches when a given expression matches, configures
   * retry, and runs the given tasks as the catch body.
   *
   * @param when jq-style condition expression
   * @param retry retry strategy configurer
   * @param doTasks one or more task configurers to execute in the catch block
   * @return a {@link TryCatchConfigurer} to be used inside {@link TrySpec}
   */
  public static TryCatchConfigurer catchWhen(
      String when, RetryConfigurer retry, TasksConfigurer... doTasks) {
    return c -> c.when(when).retry(retry).doTasks(tasks(doTasks));
  }

  /**
   * Build a {@link TryCatchConfigurer} that catches when the given expression does <em>not</em>
   * match, configures retry, and runs the given tasks as the catch body.
   *
   * @param when jq-style condition expression to exclude
   * @param retry retry strategy configurer
   * @param doTasks one or more task configurers to execute in the catch block
   * @return a {@link TryCatchConfigurer} to be used inside {@link TrySpec}
   */
  public static TryCatchConfigurer catchExceptWhen(
      String when, RetryConfigurer retry, TasksConfigurer... doTasks) {
    return c -> c.exceptWhen(when).retry(retry).doTasks(tasks(doTasks));
  }

  /**
   * Build a basic retry strategy that retries when the given condition is true and respects a
   * duration limit.
   *
   * @param when jq-style condition expression
   * @param limitDuration duration in ISO-8601 or spec-compatible format
   * @return a {@link RetryConfigurer} representing the retry strategy
   */
  public static RetryConfigurer retryWhen(String when, String limitDuration) {
    return r -> r.when(when).limit(l -> l.duration(limitDuration));
  }

  /**
   * Build a retry strategy that retries when the given condition does <em>not</em> hold and
   * respects a duration limit.
   *
   * @param when jq-style condition expression to exclude
   * @param limitDuration duration in ISO-8601 or spec-compatible format
   * @return a {@link RetryConfigurer} representing the retry strategy
   */
  public static RetryConfigurer retryExceptWhen(String when, String limitDuration) {
    return r -> r.exceptWhen(when).limit(l -> l.duration(limitDuration));
  }

  /**
   * Build an error filter for the {@code catch} clause based on a concrete error type URI and HTTP
   * status.
   *
   * @param errType error type URI
   * @param status expected HTTP status code
   * @return a consumer to configure {@link TryTaskBuilder.CatchErrorsBuilder}
   */
  public static Consumer<TryTaskBuilder.CatchErrorsBuilder> errorFilter(URI errType, int status) {
    return e -> e.type(errType.toString()).status(status);
  }

  /**
   * Build an error filter for the {@code catch} clause using a standard {@link Errors.Standard}
   * error and HTTP status.
   *
   * @param errType standard error enum
   * @param status expected HTTP status code
   * @return a consumer to configure {@link TryTaskBuilder.CatchErrorsBuilder}
   */
  public static Consumer<TryTaskBuilder.CatchErrorsBuilder> errorFilter(
      Errors.Standard errType, int status) {
    return e -> e.type(errType.toString()).status(status);
  }

  /**
   * Create an {@link AuthenticationConfigurer} that references a previously defined authentication
   * policy by name in the {@code use} block.
   *
   * <p>Equivalent to setting {@code authentication.use(name)}.
   *
   * @param authName the name of a reusable authentication policy
   * @return an {@link AuthenticationConfigurer} that sets {@code use(authName)}
   */
  public static AuthenticationConfigurer use(String authName) {
    return a -> a.use(authName);
  }

  /**
   * Build a BASIC authentication configurer with username and password.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * auth("basic-auth", basic("user", "pass"))
   * }</pre>
   *
   * @param username BASIC auth username
   * @param password BASIC auth password
   * @return an {@link AuthenticationConfigurer} for BASIC authentication
   */
  public static AuthenticationConfigurer basic(String username, String password) {
    return a -> a.basic(b -> b.username(username).password(password));
  }

  /**
   * Build a Bearer token authentication configurer.
   *
   * <p>Typical usage:
   *
   * <pre>{@code
   * auth("bearer-auth", bearer("token-123"))
   * }</pre>
   *
   * @param token bearer token
   * @return an {@link AuthenticationConfigurer} for Bearer authentication
   */
  public static AuthenticationConfigurer bearer(String token) {
    return a -> a.bearer(b -> b.token(token));
  }

  /**
   * Build a Digest authentication configurer with username and password.
   *
   * @param username digest auth username
   * @param password digest auth password
   * @return an {@link AuthenticationConfigurer} for Digest authentication
   */
  public static AuthenticationConfigurer digest(String username, String password) {
    return a -> a.digest(d -> d.username(username).password(password));
  }

  /**
   * Build an OpenID Connect (OIDC) authentication configurer without client credentials.
   *
   * @param authority OIDC authority/issuer URL
   * @param grant OAuth2 grant type
   * @return an {@link AuthenticationConfigurer} using OIDC
   */
  public static AuthenticationConfigurer oidc(
      String authority, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant) {
    return a -> a.openIDConnect(o -> o.authority(authority).grant(grant));
  }

  /**
   * Build an OpenID Connect (OIDC) authentication configurer with client credentials.
   *
   * @param authority OIDC authority/issuer URL
   * @param grant OAuth2 grant type
   * @param clientId client identifier
   * @param clientSecret client secret
   * @return an {@link AuthenticationConfigurer} using OIDC with client credentials
   */
  public static AuthenticationConfigurer oidc(
      String authority,
      OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant,
      String clientId,
      String clientSecret) {
    return a ->
        a.openIDConnect(
            o ->
                o.authority(authority)
                    .grant(grant)
                    .client(c -> c.id(clientId).secret(clientSecret)));
  }

  // TODO: we may create an OIDCSpec for chained builders if necessary

  /**
   * Alias for {@link #oidc(String, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant)} using
   * OAuth2 semantics.
   *
   * @param authority OAuth2/OIDC authority URL
   * @param grant OAuth2 grant type
   * @return an {@link AuthenticationConfigurer} configured as OAuth2
   */
  public static AuthenticationConfigurer oauth2(
      String authority, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant) {
    return a -> a.openIDConnect(o -> o.authority(authority).grant(grant));
  }

  /**
   * Alias for {@link #oidc(String, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant, String,
   * String)} using OAuth2 naming.
   *
   * @param authority OAuth2/OIDC authority URL
   * @param grant OAuth2 grant type
   * @param clientId client identifier
   * @param clientSecret client secret
   * @return an {@link AuthenticationConfigurer} configured as OAuth2 with client credentials
   */
  public static AuthenticationConfigurer oauth2(
      String authority,
      OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant,
      String clientId,
      String clientSecret) {
    return a ->
        a.openIDConnect(
            o ->
                o.authority(authority)
                    .grant(grant)
                    .client(c -> c.id(clientId).secret(clientSecret)));
  }

  /**
   * Build a {@link RaiseSpec} for an error with a string type expression and HTTP status.
   *
   * @param errExpr error type expression (URI or logical id)
   * @param status HTTP status code
   * @return a {@link RaiseSpec} configured with type and status
   */
  public static RaiseSpec error(String errExpr, int status) {
    return new RaiseSpec().type(errExpr).status(status);
  }

  /**
   * Build a {@link RaiseSpec} for an error with a string type expression.
   *
   * @param errExpr error type expression (URI or logical id)
   * @return a {@link RaiseSpec} configured with type only
   */
  public static RaiseSpec error(String errExpr) {
    return new RaiseSpec().type(errExpr);
  }

  /**
   * Build a {@link RaiseSpec} for an error using a concrete error type URI.
   *
   * @param errType error type URI
   * @return a {@link RaiseSpec} configured with type only
   */
  public static RaiseSpec error(URI errType) {
    return new RaiseSpec().type(errType);
  }

  /**
   * Build a {@link RaiseSpec} for an error using a concrete error type URI and HTTP status.
   *
   * @param errType error type URI
   * @param status HTTP status code
   * @return a {@link RaiseSpec} configured with type and status
   */
  public static RaiseSpec error(URI errType, int status) {
    return new RaiseSpec().type(errType).status(status);
  }

  // --- Errors Recipes --- //

  /**
   * Build a {@link RaiseSpec} based on a standard error enum.
   *
   * @param std standard error
   * @return a {@link RaiseSpec} using the standard error URI and status
   */
  public static RaiseSpec error(Errors.Standard std) {
    return error(std.uri(), std.status());
  }

  /**
   * Build a {@link RaiseSpec} based on a standard error enum with an overridden status.
   *
   * @param std standard error
   * @param status HTTP status code
   * @return a {@link RaiseSpec} using the standard URI and custom status
   */
  public static RaiseSpec error(Errors.Standard std, int status) {
    return error(std.uri(), status);
  }

  /**
   * Shortcut for a generic server runtime error according to {@link Errors#RUNTIME}.
   *
   * @return a {@link RaiseSpec} representing a runtime server error
   */
  public static RaiseSpec serverError() {
    return error(Errors.RUNTIME);
  }

  /**
   * Shortcut for a communication error according to {@link Errors#COMMUNICATION}.
   *
   * @return a {@link RaiseSpec} representing a communication error
   */
  public static RaiseSpec communicationError() {
    return error(Errors.COMMUNICATION);
  }

  /**
   * Shortcut for a "not implemented" error according to {@link Errors#NOT_IMPLEMENTED}.
   *
   * @return a {@link RaiseSpec} representing a not-implemented error
   */
  public static RaiseSpec notImplementedError() {
    return error(Errors.NOT_IMPLEMENTED);
  }

  /**
   * Shortcut for an unauthorized error according to {@link Errors#AUTHENTICATION}.
   *
   * @return a {@link RaiseSpec} representing an authentication error
   */
  public static RaiseSpec unauthorizedError() {
    return error(Errors.AUTHENTICATION);
  }

  /**
   * Shortcut for a forbidden error according to {@link Errors#AUTHORIZATION}.
   *
   * @return a {@link RaiseSpec} representing an authorization error
   */
  public static RaiseSpec forbiddenError() {
    return error(Errors.AUTHORIZATION);
  }

  /**
   * Shortcut for a timeout error according to {@link Errors#TIMEOUT}.
   *
   * @return a {@link RaiseSpec} representing a timeout error
   */
  public static RaiseSpec timeoutError() {
    return error(Errors.TIMEOUT);
  }

  /**
   * Shortcut for a data error according to {@link Errors#DATA}.
   *
   * @return a {@link RaiseSpec} representing a data error
   */
  public static RaiseSpec dataError() {
    return error(Errors.DATA);
  }

  // ---- Tasks ----//

  /**
   * Create a {@link TasksConfigurer} that adds an HTTP call task using a low-level HTTP configurer.
   *
   * <p>Example:
   *
   * <pre>{@code
   * tasks(
   *   call(http -> http.method("GET").endpoint("..."))
   * );
   * }</pre>
   *
   * @param configurer low-level HTTP configurer
   * @return a {@link TasksConfigurer} that adds a CallHTTP task
   */
  public static TasksConfigurer call(CallHttpConfigurer configurer) {
    return list -> list.http(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds an OpenAPI call task.
   *
   * <p>Example:
   *
   * <pre>{@code
   * tasks(
   *   call(openapi -> openapi.document("...").operation("getUser"))
   * );
   * }</pre>
   *
   * @param configurer OpenAPI configurer
   * @return a {@link TasksConfigurer} that adds a CallOpenAPI task
   */
  public static TasksConfigurer call(CallOpenAPIConfigurer configurer) {
    return list -> list.openapi(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code set} task using a low-level configurer.
   *
   * @param configurer configurer for the set task
   * @return a {@link TasksConfigurer} that adds a SetTask
   */
  public static TasksConfigurer set(SetConfigurer configurer) {
    return list -> list.set(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code set} task using a raw expression.
   *
   * @param expr expression to apply in the set task
   * @return a {@link TasksConfigurer} that adds a SetTask
   */
  public static TasksConfigurer set(String expr) {
    return list -> list.set(expr);
  }

  /**
   * Create a {@link TasksConfigurer} that adds an {@code emit} task.
   *
   * @param configurer consumer configuring {@link EmitTaskBuilder}
   * @return a {@link TasksConfigurer} that adds an EmitTask
   */
  public static TasksConfigurer emit(Consumer<EmitTaskBuilder> configurer) {
    return list -> list.emit(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code listen} task.
   *
   * @param configurer listen configurer
   * @return a {@link TasksConfigurer} that adds a ListenTask
   */
  public static TasksConfigurer listen(ListenConfigurer configurer) {
    return list -> list.listen(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code forEach} task.
   *
   * @param configurer for-each configurer
   * @return a {@link TasksConfigurer} that adds a ForEachTask
   */
  public static TasksConfigurer forEach(ForEachConfigurer configurer) {
    return list -> list.forEach(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code fork} task.
   *
   * @param configurer consumer configuring {@link ForkTaskBuilder}
   * @return a {@link TasksConfigurer} that adds a ForkTask
   */
  public static TasksConfigurer fork(Consumer<ForkTaskBuilder> configurer) {
    return list -> list.fork(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code switch} task.
   *
   * @param configurer switch configurer
   * @return a {@link TasksConfigurer} that adds a SwitchTask
   */
  public static TasksConfigurer switchCase(SwitchConfigurer configurer) {
    return list -> list.switchCase(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code raise} task.
   *
   * @param configurer raise configurer
   * @return a {@link TasksConfigurer} that adds a RaiseTask
   */
  public static TasksConfigurer raise(RaiseConfigurer configurer) {
    return list -> list.raise(configurer);
  }

  /**
   * Create a {@link TasksConfigurer} that adds a {@code try/catch} task.
   *
   * @param configurer try/catch configurer
   * @return a {@link TasksConfigurer} that adds a TryTask
   */
  public static TasksConfigurer tryCatch(TryConfigurer configurer) {
    return list -> list.tryCatch(configurer);
  }

  // ----- Tasks that requires tasks list --//

  /**
   * Main task list adapter to be used in {@code workflow().tasks()} consumers.
   *
   * <p>This wraps multiple {@link TasksConfigurer} into a single {@link Consumer} of {@link
   * DoTaskBuilder}.
   *
   * @param steps ordered list of task configurers
   * @return a consumer configuring {@link DoTaskBuilder} with the provided steps
   */
  public static Consumer<DoTaskBuilder> doTasks(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = tasks(steps);
    return d -> d.tasks(tasks);
  }

  /**
   * Task list for tasks that require a nested tasks list such as {@code for}, {@code try}, etc.
   *
   * <p>The configurers are applied in order and cloned defensively.
   *
   * @param steps ordered list of task configurers
   * @return a {@link TasksConfigurer} applying all given steps
   */
  public static TasksConfigurer tasks(TasksConfigurer... steps) {
    Objects.requireNonNull(steps, "Steps in a tasks are required");
    final List<TasksConfigurer> snapshot = List.of(steps.clone());
    return list -> snapshot.forEach(s -> s.accept(list));
  }

  /**
   * Build a {@link ForEachConfigurer} that uses a nested tasks list as its body.
   *
   * @param steps task configurers that make up the body of the for-each
   * @return a {@link ForEachConfigurer} with the given tasks
   */
  public static ForEachConfigurer forEach(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.tasks(tasks);
  }

  /**
   * Recipe for {@link io.serverlessworkflow.api.types.ForkTask} where branches compete (first to
   * complete "wins").
   *
   * <p>Configures {@code compete(true)} and sets the branches tasks list.
   *
   * @param steps task configurers shared by all branches
   * @return a {@link Consumer} configuring {@link ForkTaskBuilder}
   */
  public static Consumer<ForkTaskBuilder> branchesCompete(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.compete(true).branches(tasks);
  }

  /**
   * Recipe for {@link io.serverlessworkflow.api.types.ForkTask} where branches do not compete (all
   * branches are executed).
   *
   * <p>Configures {@code compete(false)} and sets the branches tasks list.
   *
   * @param steps task configurers shared by all branches
   * @return a {@link Consumer} configuring {@link ForkTaskBuilder}
   */
  public static Consumer<ForkTaskBuilder> branches(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.compete(false).branches(tasks);
  }
}
