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
import io.serverlessworkflow.api.types.Shell;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RunShellExecutor implements RunnableTask<RunShell> {

  private ShellResultSupplier shellResultSupplier;
  private ProcessBuilderSupplier processBuilderSupplier;

  @FunctionalInterface
  private interface ShellResultSupplier {
    WorkflowModel apply(
        TaskContext taskContext, WorkflowModel input, ProcessBuilder processBuilder);
  }

  @FunctionalInterface
  private interface ProcessBuilderSupplier {
    ProcessBuilder apply(WorkflowContext workflowContext, TaskContext taskContext);
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    ProcessBuilder processBuilder = this.processBuilderSupplier.apply(workflowContext, taskContext);
    return CompletableFuture.supplyAsync(
        () -> this.shellResultSupplier.apply(taskContext, input, processBuilder));
  }

  @Override
  public void init(RunShell taskConfiguration, WorkflowDefinition definition) {
    Shell shell = taskConfiguration.getShell();
    final String shellCommand = shell.getCommand();

    if (shellCommand == null || shellCommand.isBlank()) {
      throw new IllegalStateException("Missing shell command in RunShell task configuration");
    }
    this.processBuilderSupplier =
        (workflowContext, taskContext) -> {
          WorkflowApplication application = definition.application();

          StringBuilder commandBuilder =
              new StringBuilder(
                  ExpressionUtils.isExpr(shellCommand)
                      ? WorkflowUtils.buildStringFilter(application, shellCommand)
                          .apply(workflowContext, taskContext, taskContext.input())
                      : shellCommand);

          if (shell.getArguments() != null
              && shell.getArguments().getAdditionalProperties() != null) {
            for (Map.Entry<String, Object> entry :
                shell.getArguments().getAdditionalProperties().entrySet()) {
              commandBuilder
                  .append(" ")
                  .append(
                      ExpressionUtils.isExpr(entry.getKey())
                          ? WorkflowUtils.buildStringFilter(application, entry.getKey())
                              .apply(workflowContext, taskContext, taskContext.input())
                          : entry.getKey());
              if (entry.getValue() != null) {

                commandBuilder
                    .append("=")
                    .append(
                        ExpressionUtils.isExpr(entry.getValue())
                            ? WorkflowUtils.buildStringFilter(
                                    application, entry.getValue().toString())
                                .apply(workflowContext, taskContext, taskContext.input())
                            : entry.getValue().toString());
              }
            }
          }

          // TODO: support Windows cmd.exe
          ProcessBuilder builder = new ProcessBuilder("sh", "-c", commandBuilder.toString());
          if (shell.getEnvironment() != null
              && shell.getEnvironment().getAdditionalProperties() != null) {
            for (Map.Entry<String, Object> entry :
                shell.getEnvironment().getAdditionalProperties().entrySet()) {
              String value =
                  ExpressionUtils.isExpr(entry.getValue())
                      ? WorkflowUtils.buildStringFilter(application, entry.getValue().toString())
                          .apply(workflowContext, taskContext, taskContext.input())
                      : entry.getValue().toString();

              // configure environments
              builder.environment().put(entry.getKey(), value);
            }
          }
          return builder;
        };

    this.shellResultSupplier =
        (taskContext, input, processBuilder) -> {
          try {
            Process process = processBuilder.start();
            return taskConfiguration.isAwait()
                ? buildResultFromProcess(taskConfiguration, definition, process)
                : input;
          } catch (IOException | InterruptedException e) {
            throw new WorkflowException(WorkflowError.runtime(taskContext, e).build(), e);
          }
        };
  }

  /**
   * Builds the WorkflowModel result from the executed process. It waits for the process to finish
   * and captures the exit code, stdout, and stderr based on the task configuration.
   */
  private WorkflowModel buildResultFromProcess(
      RunShell taskConfiguration, WorkflowDefinition definition, Process process)
      throws IOException, InterruptedException {
    int exitCode = process.waitFor();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

    WorkflowModelFactory modelFactory = definition.application().modelFactory();
    return switch (taskConfiguration.getReturn()) {
      case ALL -> modelFactory.fromAny(new ProcessResult(exitCode, stdout.trim(), stderr.trim()));
      case NONE -> modelFactory.fromNull();
      case CODE -> modelFactory.from(exitCode);
      case STDOUT -> modelFactory.from(stdout.trim());
      case STDERR -> modelFactory.from(stderr.trim());
    };
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunShell.class.equals(clazz);
  }
}
