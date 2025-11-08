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
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.resources.ResourceLoaderUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public class RunScriptExecutor implements RunnableTask<RunScript> {

  public enum LanguageId {
    JS("js"),
    PYTHON("python");

    private final String lang;

    LanguageId(String lang) {
      this.lang = lang;
    }

    public String getLang() {
      return lang;
    }

    public static boolean isSupported(String lang) {
      for (LanguageId l : LanguageId.values()) {
        if (l.getLang().equalsIgnoreCase(lang)) {
          return true;
        }
      }
      return false;
    }
  }

  @FunctionalInterface
  private interface CodeSupplier {
    String apply(WorkflowContext workflowContext, TaskContext taskContext);
  }

  @SuppressWarnings("rawtypes")
  private Map<String, WorkflowValueResolver> environmentExpr;

  @SuppressWarnings("rawtypes")
  private Map<String, WorkflowValueResolver> argumentExpr;

  private CodeSupplier codeSupplier;
  private boolean isAwait;
  private RunTaskConfiguration.ProcessReturnType returnType;
  private ScriptTaskRunner taskRunner;

  @Override
  public void init(RunScript taskConfiguration, WorkflowDefinition definition) {
    ScriptUnion scriptUnion = taskConfiguration.getScript();
    Script script = scriptUnion.get();
    String language = scriptUnion.get().getLanguage();

    WorkflowApplication application = definition.application();
    if (language == null || !LanguageId.isSupported(language)) {
      throw new IllegalArgumentException(
          "Unsupported script language: "
              + language
              + ". Supported languages are: "
              + Arrays.toString(
                  Arrays.stream(LanguageId.values()).map(LanguageId::getLang).toArray()));
    }

    this.taskRunner =
        ServiceLoader.load(ScriptTaskRunner.class)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No implementation found for ScriptTaskRunner"));

    this.isAwait = taskConfiguration.isAwait();

    this.returnType = taskConfiguration.getReturn();

    if (script.getEnvironment() != null
        && script.getEnvironment().getAdditionalProperties() != null) {
      this.environmentExpr =
          buildMapResolvers(application, script.getEnvironment().getAdditionalProperties());
    } else {
      this.environmentExpr = Map.of();
    }

    if (script.getArguments() != null && script.getArguments().getAdditionalProperties() != null) {
      this.argumentExpr =
          buildMapResolvers(application, script.getArguments().getAdditionalProperties());
    } else {
      this.argumentExpr = Map.of();
    }

    this.codeSupplier =
        (workflowContext, taskContext) -> {
          if (scriptUnion.getInlineScript() != null) {
            return scriptUnion.getInlineScript().getCode();
          } else if (scriptUnion.getExternalScript() == null) {
            throw new WorkflowException(
                WorkflowError.runtime(
                        taskContext, new IllegalStateException("No script source defined."))
                    .build());
          } else {
            return definition
                .resourceLoader()
                .load(
                    scriptUnion.getExternalScript().getSource(),
                    ResourceLoaderUtils::readString,
                    workflowContext,
                    taskContext,
                    taskContext.input());
          }
        };
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    RunScriptContext.RunScriptContextBuilder builder =
        new RunScriptContext.RunScriptContextBuilder();

    Map<String, String> envs = new HashMap<>();
    this.environmentExpr.forEach(
        (k, v) -> {
          Object resolved = v.apply(workflowContext, taskContext, input);
          envs.put(k, resolved.toString());
        });

    Map<String, Object> args = new HashMap<>();
    this.argumentExpr.forEach(
        (k, v) -> {
          Object resolved = v.apply(workflowContext, taskContext, input);
          args.put(k, resolved);
        });

    String code = this.codeSupplier.apply(workflowContext, taskContext);

    RunScriptContext scriptContext =
        builder
            .withApplication(workflowContext.definition().application())
            .withReturnType(returnType)
            .withCode(code)
            .withArguments(args)
            .withEnvironment(envs)
            .withAwait(isAwait)
            .build();

    return CompletableFuture.supplyAsync(
        () -> taskRunner.buildRun(taskContext).apply(scriptContext, input));
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunScript.class.equals(clazz);
  }

  /** Builds a map of WorkflowValueResolvers from the provided properties. */
  @SuppressWarnings("rawtypes")
  private Map<String, WorkflowValueResolver> buildMapResolvers(
      WorkflowApplication application, Map<String, Object> properties) {
    Map<String, WorkflowValueResolver> resolvers = new HashMap<>();
    if (properties != null) {
      for (Map.Entry<String, Object> entry : properties.entrySet()) {
        WorkflowValueResolver<String> valueResolver =
            WorkflowUtils.buildStringFilter(application, entry.getValue().toString());
        resolvers.put(entry.getKey(), valueResolver);
      }
    }
    return resolvers;
  }

  public static class RunScriptContext {
    private final WorkflowApplication application;
    private final Map<String, Object> args;
    private final Map<String, String> envs;
    private final String code;
    private final boolean isAwait;
    private final RunTaskConfiguration.ProcessReturnType returnType;

    public RunScriptContext(RunScriptContextBuilder builder) {
      this.application = builder.application;
      this.args = builder.args;
      this.envs = builder.envs;
      this.code = builder.code;
      this.isAwait = builder.awaiting;
      this.returnType = builder.returnType;
    }

    public Map<String, Object> getArgs() {
      return args;
    }

    public Map<String, String> getEnvs() {
      return envs;
    }

    public String getCode() {
      return code;
    }

    public boolean isAwait() {
      return isAwait;
    }

    public WorkflowApplication getApplication() {
      return application;
    }

    public RunTaskConfiguration.ProcessReturnType getReturnType() {
      return returnType;
    }

    public static class RunScriptContextBuilder {
      private Map<String, Object> args;
      private Map<String, String> envs;
      private String code;
      private boolean awaiting;
      private WorkflowApplication application;
      private RunTaskConfiguration.ProcessReturnType returnType;

      public RunScriptContextBuilder withArguments(Map<String, Object> args) {
        this.args = args;
        return this;
      }

      public RunScriptContextBuilder withEnvironment(Map<String, String> envs) {
        this.envs = envs;
        return this;
      }

      public RunScriptContextBuilder withCode(String code) {
        this.code = code;
        return this;
      }

      public RunScriptContextBuilder withAwait(boolean awaiting) {
        this.awaiting = awaiting;
        return this;
      }

      public RunScriptContextBuilder withApplication(WorkflowApplication application) {
        this.application = application;
        return this;
      }

      public RunScriptContextBuilder withReturnType(
          RunTaskConfiguration.ProcessReturnType returnType) {
        this.returnType = returnType;
        return this;
      }

      public RunScriptContext build() {
        return new RunScriptContext(this);
      }
    }
  }
}
