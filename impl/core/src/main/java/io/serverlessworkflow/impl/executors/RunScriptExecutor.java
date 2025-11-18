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
package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.RunScript;
import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.api.types.Script;
import io.serverlessworkflow.api.types.ScriptUnion;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.resources.ResourceLoaderUtils;
import io.serverlessworkflow.impl.scripts.ScriptContext;
import io.serverlessworkflow.impl.scripts.ScriptLanguageId;
import io.serverlessworkflow.impl.scripts.ScriptRunner;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public class RunScriptExecutor implements RunnableTask<RunScript> {

  private Optional<WorkflowValueResolver<Map<String, Object>>> environmentExpr;
  private Optional<WorkflowValueResolver<Map<String, Object>>> argumentExpr;
  private WorkflowValueResolver<String> codeSupplier;
  private boolean isAwait;
  private Optional<RunTaskConfiguration.ProcessReturnType> returnType;
  private ScriptRunner taskRunner;

  @Override
  public void init(RunScript taskConfiguration, WorkflowDefinition definition) {
    ScriptUnion scriptUnion = taskConfiguration.getScript();
    Script script = scriptUnion.get();
    ScriptLanguageId language = ScriptLanguageId.from(script.getLanguage());

    this.taskRunner =
        ServiceLoader.load(ScriptRunner.class).stream()
            .map(ServiceLoader.Provider::get)
            .filter(s -> s.identifier().equals(language))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No script runner implementation found for language " + language));

    this.isAwait = taskConfiguration.isAwait();

    this.returnType = Optional.ofNullable(taskConfiguration.getReturn());

    WorkflowApplication application = definition.application();
    this.environmentExpr =
        script.getEnvironment() != null && script.getEnvironment().getAdditionalProperties() != null
            ? Optional.of(
                WorkflowUtils.buildMapResolver(
                    application, script.getEnvironment().getAdditionalProperties()))
            : Optional.empty();

    this.argumentExpr =
        script.getArguments() != null && script.getArguments().getAdditionalProperties() != null
            ? Optional.of(
                WorkflowUtils.buildMapResolver(
                    application, script.getArguments().getAdditionalProperties()))
            : Optional.empty();

    this.codeSupplier =
        scriptUnion.getInlineScript() != null
            ? WorkflowUtils.buildStringFilter(application, scriptUnion.getInlineScript().getCode())
            : (w, t, m) ->
                definition
                    .resourceLoader()
                    .load(
                        Objects.requireNonNull(
                                scriptUnion.getExternalScript(),
                                "External script is required if inline script was not set")
                            .getSource(),
                        ResourceLoaderUtils::readString,
                        w,
                        t,
                        m);
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    ScriptContext scriptContext =
        new ScriptContext(
            argumentExpr.map(m -> m.apply(workflowContext, taskContext, input)).orElse(Map.of()),
            environmentExpr.map(m -> m.apply(workflowContext, taskContext, input)).orElse(Map.of()),
            codeSupplier.apply(workflowContext, taskContext, input),
            returnType);
    if (isAwait) {
      return CompletableFuture.supplyAsync(
          () -> taskRunner.runScript(scriptContext, workflowContext, taskContext, input),
          workflowContext.definition().application().executorService());
    } else {
      workflowContext
          .definition()
          .application()
          .executorService()
          .submit(() -> taskRunner.runScript(scriptContext, workflowContext, taskContext, input));
      return CompletableFuture.completedFuture(input);
    }
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunScript.class.equals(clazz);
  }
}
