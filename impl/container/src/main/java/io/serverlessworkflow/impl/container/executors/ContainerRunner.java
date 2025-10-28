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
package io.serverlessworkflow.impl.container.executors;

import static io.serverlessworkflow.api.types.ContainerLifetime.ContainerCleanupPolicy.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class ContainerRunner {

  private static final DefaultDockerClientConfig DEFAULT_CONFIG =
      DefaultDockerClientConfig.createDefaultConfigBuilder().build();

  private static final DockerClient dockerClient =
      DockerClientImpl.getInstance(
          DEFAULT_CONFIG,
          new ApacheDockerHttpClient.Builder().dockerHost(DEFAULT_CONFIG.getDockerHost()).build());

  private final CreateContainerCmd createContainerCmd;
  private final Container container;
  private final List<ContainerPropertySetter> propertySetters;
  private final WorkflowDefinition definition;

  private ContainerRunner(
      CreateContainerCmd createContainerCmd, WorkflowDefinition definition, Container container) {
    this.createContainerCmd = createContainerCmd;
    this.definition = definition;
    this.container = container;
    this.propertySetters = new ArrayList<>();
  }

  /**
   * Blocking container execution according to the lifetime policy. Returns an already completed
   * CompletableFuture: - completedFuture(input) if exitCode == 0 - exceptionally completed if the
   * exit code is non-zero or an error occurs. The method blocks the calling thread until the
   * container finishes or the timeout expires.
   */
  CompletableFuture<WorkflowModel> startSync(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {

    StringExpressionResolver resolver =
        new StringExpressionResolver(workflowContext, taskContext, input);

    propertySetters.forEach(setter -> setter.accept(resolver));

    CreateContainerResponse createContainerResponse = createContainerCmd.exec();
    String containerId = createContainerResponse.getId();

    if (containerId == null || containerId.isEmpty()) {
      return failed("Container creation failed: empty container ID");
    }

    dockerClient.startContainerCmd(containerId).exec();

    int exitCode;
    try (WaitContainerResultCallback resultCallback =
        dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())) {
      if (container.getLifetime() != null
          && container.getLifetime().getCleanup() != null
          && container.getLifetime().getCleanup().equals(EVENTUALLY)) {
        try {
          WorkflowValueResolver<Duration> durationResolver =
              WorkflowUtils.fromTimeoutAfter(
                  definition.application(), container.getLifetime().getAfter());
          Duration timeout = durationResolver.apply(workflowContext, taskContext, input);
          exitCode = resultCallback.awaitStatusCode(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (DockerClientException e) {
          return failed(
              String.format("Error while waiting for container to finish: %s ", e.getMessage()));
        } finally {
          dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        }
      } else {
        exitCode = resultCallback.awaitStatusCode();
      }
    } catch (IOException e) {
      return failed(
          String.format("Error while waiting for container to finish: %s ", e.getMessage()));
    }

    return switch (exitCode) {
      case 0 -> CompletableFuture.completedFuture(input);
      case 1 -> failed("General error (exit code 1)");
      case 2 -> failed("Shell syntax error (exit code 2)");
      case 126 -> failed("Command found but not executable (exit code 126)");
      case 127 -> failed("Command not found (exit code 127)");
      case 130 -> failed("Interrupted by SIGINT (exit code 130)");
      case 137 -> failed("Killed by SIGKILL (exit code 137)");
      case 139 -> failed("Segmentation fault (exit code 139)");
      case 143 -> failed("Terminated by SIGTERM (exit code 143)");
      default -> failed("Process exited with code " + exitCode);
    };
  }

  private static <T> CompletableFuture<T> failed(String message) {
    CompletableFuture<T> f = new CompletableFuture<>();
    f.completeExceptionally(new RuntimeException(message));
    return f;
  }

  static ContainerRunnerBuilder builder() {
    return new ContainerRunnerBuilder();
  }

  public static class ContainerRunnerBuilder {
    private Container container = null;
    private WorkflowDefinition workflowDefinition;

    private ContainerRunnerBuilder() {}

    ContainerRunnerBuilder withContainer(Container container) {
      this.container = container;
      return this;
    }

    public ContainerRunnerBuilder withWorkflowDefinition(WorkflowDefinition definition) {
      this.workflowDefinition = definition;
      return this;
    }

    ContainerRunner build() {
      if (container.getImage() == null || container.getImage().isEmpty()) {
        throw new IllegalArgumentException("Container image must be provided");
      }

      CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(container.getImage());

      ContainerRunner runner =
          new ContainerRunner(createContainerCmd, workflowDefinition, container);

      runner.propertySetters.add(new CommandPropertySetter(createContainerCmd, container));
      runner.propertySetters.add(
          new ContainerEnvironmentPropertySetter(createContainerCmd, container));
      runner.propertySetters.add(new NamePropertySetter(createContainerCmd, container));
      runner.propertySetters.add(new PortsPropertySetter(createContainerCmd, container));
      runner.propertySetters.add(new VolumesPropertySetter(createContainerCmd, container));
      runner.propertySetters.add(new LifetimePropertySetter(createContainerCmd, container));
      return runner;
    }
  }
}
