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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
    WorkflowModel workflowModel =
        this.shellResultSupplier.apply(taskContext, input, processBuilder);
    return CompletableFuture.completedFuture(workflowModel);
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

          String command =
              ExpressionUtils.isExpr(shellCommand)
                  ? WorkflowUtils.buildStringResolver(
                          application, shellCommand, taskContext.input().asJavaObject())
                      .apply(workflowContext, taskContext, taskContext.input())
                  : shellCommand;

          StringBuilder commandBuilder = new StringBuilder(command);

          if (shell.getArguments() != null
              && shell.getArguments().getAdditionalProperties() != null) {
            for (Map.Entry<String, Object> entry :
                shell.getArguments().getAdditionalProperties().entrySet()) {

              String argKey =
                  ExpressionUtils.isExpr(entry.getKey())
                      ? WorkflowUtils.buildStringResolver(
                              application, entry.getKey(), taskContext.input().asJavaObject())
                          .apply(workflowContext, taskContext, taskContext.input())
                      : entry.getKey();

              if (entry.getValue() == null) {
                commandBuilder.append(" ").append(argKey);
                continue;
              }

              String argValue =
                  ExpressionUtils.isExpr(entry.getValue())
                      ? WorkflowUtils.buildStringResolver(
                              application,
                              entry.getValue().toString(),
                              taskContext.input().asJavaObject())
                          .apply(workflowContext, taskContext, taskContext.input())
                      : entry.getValue().toString();

              commandBuilder.append(" ").append(argKey).append("=").append(argValue);
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
                      ? WorkflowUtils.buildStringResolver(
                              application,
                              entry.getValue().toString(),
                              taskContext.input().asJavaObject())
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

            if (taskConfiguration.isAwait()) {
              return waitForResult(taskConfiguration, definition, process);
            } else {
              return input;
            }

          } catch (IOException | InterruptedException e) {
            throw new WorkflowException(WorkflowError.runtime(taskContext, e).build(), e);
          }
        };
  }

  private WorkflowModel waitForResult(
      RunShell taskConfiguration, WorkflowDefinition definition, Process process)
      throws IOException, InterruptedException {

    CompletableFuture<String> futureStdout =
        CompletableFuture.supplyAsync(() -> readInputStream(process.getInputStream()));
    CompletableFuture<String> futureStderr =
        CompletableFuture.supplyAsync(() -> readInputStream(process.getErrorStream()));

    int exitCode = process.waitFor();

    CompletableFuture<Void> allStd = CompletableFuture.allOf(futureStdout, futureStderr);

    allStd.join();

    String stdout = futureStdout.join();
    String stderr = futureStderr.join();

    RunTaskConfiguration.ProcessReturnType returnType = taskConfiguration.getReturn();

    WorkflowModelFactory modelFactory = definition.application().modelFactory();

    return switch (returnType) {
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

  /**
   * Reads an InputStream and returns its content as a String. It keeps the original content using
   * UTF-8 encoding.
   *
   * @param inputStream {@link InputStream} to be read
   * @return {@link String} with the content of the InputStream
   */
  public static String readInputStream(InputStream inputStream) {
    StringWriter writer = new StringWriter();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      char[] buffer = new char[1024];
      int charsRead;
      while ((charsRead = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, charsRead);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }
}
