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

import io.serverlessworkflow.api.types.RunShell;
import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.api.types.RunTaskConfiguration.ProcessReturnType;
import io.serverlessworkflow.api.types.Shell;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RunShellExecutor implements RunnableTask<RunShell> {

  private WorkflowValueResolver<String> shellCommand;
  private Map<WorkflowValueResolver<String>, Optional<WorkflowValueResolver<String>>>
      shellArguments;
  private Optional<WorkflowValueResolver<Map<String, Object>>> shellEnv;
  private Optional<ProcessReturnType> returnType;

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel model) {
    StringBuilder commandBuilder =
        new StringBuilder(shellCommand.apply(workflowContext, taskContext, model));
    for (var entry : shellArguments.entrySet()) {
      commandBuilder.append(" ").append(entry.getKey().apply(workflowContext, taskContext, model));
      entry
          .getValue()
          .ifPresent(
              v -> commandBuilder.append("=").append(v.apply(workflowContext, taskContext, model)));
    }

    ProcessBuilder builder = new ProcessBuilder("sh", "-c", commandBuilder.toString());
    shellEnv.ifPresent(
        map -> {
          for (Map.Entry<String, Object> entry :
              map.apply(workflowContext, taskContext, model).entrySet()) {
            builder.environment().put(entry.getKey(), (String) entry.getValue());
          }
        });

    return returnType
        .map(
            type ->
                CompletableFuture.supplyAsync(
                    () ->
                        buildResultFromProcess(
                                workflowContext.definition().application().modelFactory(),
                                uncheckedStart(builder),
                                type)
                            .orElse(model),
                    workflowContext.definition().application().executorService()))
        .orElseGet(
            () -> {
              workflowContext
                  .definition()
                  .application()
                  .executorService()
                  .submit(() -> uncheckedStart(builder));
              return CompletableFuture.completedFuture(model);
            });
  }

  private Process uncheckedStart(ProcessBuilder builder) {
    try {
      return builder.start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void init(RunShell taskConfiguration, WorkflowDefinition definition) {
    Shell shell = taskConfiguration.getShell();
    if (!WorkflowUtils.isValid(taskConfiguration.getShell().getCommand())) {
      throw new IllegalStateException("Missing shell command in RunShell task configuration");
    }
    shellCommand =
        WorkflowUtils.buildStringFilter(
            definition.application(), taskConfiguration.getShell().getCommand());

    shellArguments =
        shell.getArguments() != null && shell.getArguments().getAdditionalProperties() != null
            ? shell.getArguments().getAdditionalProperties().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        e -> WorkflowUtils.buildStringFilter(definition.application(), e.getKey()),
                        e ->
                            e.getValue() != null
                                ? Optional.of(
                                    WorkflowUtils.buildStringFilter(
                                        definition.application(), e.getValue().toString()))
                                : Optional.empty(),
                        (x, y) -> y,
                        LinkedHashMap::new))
            : Map.of();

    shellEnv =
        shell.getEnvironment() != null && shell.getEnvironment().getAdditionalProperties() != null
            ? Optional.of(
                WorkflowUtils.buildMapResolver(
                    definition.application(), shell.getEnvironment().getAdditionalProperties()))
            : Optional.empty();

    returnType =
        taskConfiguration.isAwait() ? Optional.of(taskConfiguration.getReturn()) : Optional.empty();
  }

  private static Optional<WorkflowModel> buildResultFromProcess(
      WorkflowModelFactory modelFactory, Process process, ProcessReturnType type) {
    try {
      int exitCode = process.waitFor();
      String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
      return Optional.of(
          switch (type) {
            case ALL ->
                modelFactory.fromAny(new ProcessResult(exitCode, stdout.trim(), stderr.trim()));
            case NONE -> modelFactory.fromNull();
            case CODE -> modelFactory.from(exitCode);
            case STDOUT -> modelFactory.from(stdout.trim());
            case STDERR -> modelFactory.from(stderr.trim());
          });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunShell.class.equals(clazz);
  }
}
