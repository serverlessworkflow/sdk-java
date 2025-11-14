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

public final class DSL {

  private DSL() {}

  // ---- Convenient shortcuts ----//

  public static CallHttpSpec http() {
    return new CallHttpSpec();
  }

  public static CallOpenAPISpec openapi() {
    return new CallOpenAPISpec();
  }

  public static SwitchSpec cases() {
    return new SwitchSpec();
  }

  public static ListenSpec to() {
    return new ListenSpec();
  }

  public static EventSpec event() {
    return new EventSpec();
  }

  public static TrySpec tryCatch() {
    return new TrySpec();
  }

  public static TryCatchConfigurer catchWhen(
      String when, RetryConfigurer retry, TasksConfigurer... doTasks) {
    return c -> c.when(when).retry(retry).doTasks(tasks(doTasks));
  }

  public static TryCatchConfigurer catchExceptWhen(
      String when, RetryConfigurer retry, TasksConfigurer... doTasks) {
    return c -> c.exceptWhen(when).retry(retry).doTasks(tasks(doTasks));
  }

  public static RetryConfigurer retryWhen(String when, String limitDuration) {
    return r -> r.when(when).limit(l -> l.duration(limitDuration));
  }

  public static RetryConfigurer retryExceptWhen(String when, String limitDuration) {
    return r -> r.exceptWhen(when).limit(l -> l.duration(limitDuration));
  }

  public static Consumer<TryTaskBuilder.CatchErrorsBuilder> errorFilter(URI errType, int status) {
    return e -> e.type(errType.toString()).status(status);
  }

  public static Consumer<TryTaskBuilder.CatchErrorsBuilder> errorFilter(
      Errors.Standard errType, int status) {
    return e -> e.type(errType.toString()).status(status);
  }

  public static AuthenticationConfigurer auth(String authName) {
    return a -> a.use(authName);
  }

  public static AuthenticationConfigurer basic(String username, String password) {
    return a -> a.basic(b -> b.username(username).password(password));
  }

  public static AuthenticationConfigurer bearer(String token) {
    return a -> a.bearer(b -> b.token(token));
  }

  public static AuthenticationConfigurer digest(String username, String password) {
    return a -> a.digest(d -> d.username(username).password(password));
  }

  public static AuthenticationConfigurer oidc(
      String authority, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant) {
    return a -> a.openIDConnect(o -> o.authority(authority).grant(grant));
  }

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

  public static AuthenticationConfigurer oauth2(
      String authority, OAuth2AuthenticationData.OAuth2AuthenticationDataGrant grant) {
    return a -> a.openIDConnect(o -> o.authority(authority).grant(grant));
  }

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

  public static RaiseSpec error(String errExpr, int status) {
    return new RaiseSpec().type(errExpr).status(status);
  }

  public static RaiseSpec error(String errExpr) {
    return new RaiseSpec().type(errExpr);
  }

  public static RaiseSpec error(URI errType) {
    return new RaiseSpec().type(errType);
  }

  public static RaiseSpec error(URI errType, int status) {
    return new RaiseSpec().type(errType).status(status);
  }

  // --- Errors Recipes --- //
  public static RaiseSpec error(Errors.Standard std) {
    return error(std.uri(), std.status());
  }

  public static RaiseSpec error(Errors.Standard std, int status) {
    return error(std.uri(), status);
  }

  public static RaiseSpec serverError() {
    return error(Errors.RUNTIME);
  }

  public static RaiseSpec communicationError() {
    return error(Errors.COMMUNICATION);
  }

  public static RaiseSpec notImplementedError() {
    return error(Errors.NOT_IMPLEMENTED);
  }

  public static RaiseSpec unauthorizedError() {
    return error(Errors.AUTHENTICATION);
  }

  public static RaiseSpec forbiddenError() {
    return error(Errors.AUTHORIZATION);
  }

  public static RaiseSpec timeoutError() {
    return error(Errors.TIMEOUT);
  }

  public static RaiseSpec dataError() {
    return error(Errors.DATA);
  }

  // ---- Tasks ----//

  public static TasksConfigurer call(CallHttpConfigurer configurer) {
    return list -> list.http(configurer);
  }

  public static TasksConfigurer call(CallOpenAPIConfigurer configurer) {
    return list -> list.openapi(configurer);
  }

  public static TasksConfigurer set(SetConfigurer configurer) {
    return list -> list.set(configurer);
  }

  public static TasksConfigurer set(String expr) {
    return list -> list.set(expr);
  }

  public static TasksConfigurer emit(Consumer<EmitTaskBuilder> configurer) {
    return list -> list.emit(configurer);
  }

  public static TasksConfigurer listen(ListenConfigurer configurer) {
    return list -> list.listen(configurer);
  }

  public static TasksConfigurer forEach(ForEachConfigurer configurer) {
    return list -> list.forEach(configurer);
  }

  public static TasksConfigurer fork(Consumer<ForkTaskBuilder> configurer) {
    return list -> list.fork(configurer);
  }

  public static TasksConfigurer switchCase(SwitchConfigurer configurer) {
    return list -> list.switchCase(configurer);
  }

  public static TasksConfigurer raise(RaiseConfigurer configurer) {
    return list -> list.raise(configurer);
  }

  public static TasksConfigurer tryCatch(TryConfigurer configurer) {
    return list -> list.tryCatch(configurer);
  }

  // ----- Tasks that requires tasks list --//

  /** Main task list to be used in `workflow().tasks()` consumer. */
  public static Consumer<DoTaskBuilder> doTasks(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = tasks(steps);
    return d -> d.tasks(tasks);
  }

  /** Task list for tasks that requires it such as `for`, `try`, and so on. */
  public static TasksConfigurer tasks(TasksConfigurer... steps) {
    Objects.requireNonNull(steps, "Steps in a tasks are required");
    final List<TasksConfigurer> snapshot = List.of(steps.clone());
    return list -> snapshot.forEach(s -> s.accept(list));
  }

  public static ForEachConfigurer forEach(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.tasks(tasks);
  }

  /** Recipe for {@link io.serverlessworkflow.api.types.ForkTask} branch that DO compete */
  public static Consumer<ForkTaskBuilder> branchesCompete(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.compete(true).branches(tasks);
  }

  /** Recipe for {@link io.serverlessworkflow.api.types.ForkTask} branch that DO NOT compete */
  public static Consumer<ForkTaskBuilder> branches(TasksConfigurer... steps) {
    final Consumer<TaskItemListBuilder> tasks = DSL.tasks(steps);
    return f -> f.compete(false).branches(tasks);
  }
}
