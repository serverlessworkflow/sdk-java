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

import static io.serverlessworkflow.api.types.ContainerLifetime.*;
import static io.serverlessworkflow.impl.WorkflowUtils.isValid;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.NameParser;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.serverlessworkflow.api.types.ContainerLifetime.ContainerCleanupPolicy;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class ContainerRunner implements CallableTask {

  private static final DefaultDockerClientConfig DEFAULT_CONFIG =
      DefaultDockerClientConfig.createDefaultConfigBuilder().build();

  private static class DockerClientHolder {
    private static final DockerClient dockerClient =
        DockerClientImpl.getInstance(
            DEFAULT_CONFIG,
            new ApacheDockerHttpClient.Builder()
                .dockerHost(DEFAULT_CONFIG.getDockerHost())
                .build());
  }

  private final Collection<ContainerPropertySetter> propertySetters;
  private final Optional<WorkflowValueResolver<Duration>> timeout;
  private final ContainerCleanupPolicy policy;
  private final String containerImage;

  public ContainerRunner(
      Collection<ContainerPropertySetter> propertySetters,
      Optional<WorkflowValueResolver<Duration>> timeout,
      ContainerCleanupPolicy policy,
      String containerImage) {
    this.propertySetters = propertySetters;
    this.timeout = timeout;
    this.policy = policy;
    this.containerImage = containerImage;
  }

  @Override
  public CompletableFuture<WorkflowModel> apply(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    return CompletableFuture.supplyAsync(
        () -> startSync(workflowContext, taskContext, input),
        workflowContext.definition().application().executorService());
  }

  private WorkflowModel startSync(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    Integer exit = executeContainer(workflowContext, taskContext, input);
    if (exit == null || exit == 0) {
      return input;
    } else {
      throw mapExitCode(exit);
    }
  }

  private Integer executeContainer(
      WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input) {
    try {
      pullImageIfNeeded(containerImage);
      CreateContainerCmd containerCommand =
          DockerClientHolder.dockerClient.createContainerCmd(containerImage);
      propertySetters.forEach(p -> p.accept(containerCommand, workflowContext, taskContext, input));
      return waitAccordingToLifetime(
          createAndStartContainer(containerCommand), workflowContext, taskContext, input);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw failed("Container execution failed with exit code " + ie.getMessage());
    } catch (IOException e) {
      throw failed("Container execution failed with exit code " + e.getMessage());
    }
  }

  private void pullImageIfNeeded(String imageRef) throws InterruptedException {
    NameParser.ReposTag rt = NameParser.parseRepositoryTag(imageRef);
    DockerClientHolder.dockerClient
        .pullImageCmd(NameParser.resolveRepositoryName(imageRef).reposName)
        .withTag(WorkflowUtils.isValid(rt.tag) ? rt.tag : "latest")
        .start()
        .awaitCompletion();
  }

  private String createAndStartContainer(CreateContainerCmd containerCommand) {
    CreateContainerResponse resp = containerCommand.exec();
    String id = resp.getId();
    if (!isValid(id)) {
      throw new IllegalStateException("Container creation failed: empty ID");
    }
    DockerClientHolder.dockerClient.startContainerCmd(id).exec();
    return id;
  }

  private Integer waitAccordingToLifetime(
      String id, WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input)
      throws IOException {
    try (var cb =
        DockerClientHolder.dockerClient
            .waitContainerCmd(id)
            .exec(new WaitContainerResultCallback())) {
      if (policy == ContainerCleanupPolicy.EVENTUALLY) {
        Duration timeout =
            this.timeout
                .map(t -> t.apply(workflowContext, taskContext, input))
                .orElse(Duration.ZERO);
        try {
          Integer exit = cb.awaitStatusCode(timeout.toMillis(), TimeUnit.MILLISECONDS);
          safeStop(id);
          return exit;
        } catch (DockerClientException timeoutOrOther) {
          safeStop(id);
        }
      } else {
        return cb.awaitStatusCode();
      }
    } catch (NotFoundException e) {
      // container already removed
    }
    return 0;
  }

  private boolean isRunning(String id) {
    try {
      var st = DockerClientHolder.dockerClient.inspectContainerCmd(id).exec().getState();
      return st != null && Boolean.TRUE.equals(st.getRunning());
    } catch (Exception e) {
      return false; // must be already removed
    }
  }

  private void safeStop(String id) {
    if (isRunning(id)) {
      safeStop(id, Duration.ofSeconds(10));
      try (var cb2 =
          DockerClientHolder.dockerClient
              .waitContainerCmd(id)
              .exec(new WaitContainerResultCallback())) {
        cb2.awaitStatusCode();
        safeRemove(id);
      } catch (Exception ignore) {
        // we can ignore this
      }
    } else {
      safeRemove(id);
    }
  }

  private void safeStop(String id, Duration timeout) {
    try {
      DockerClientHolder.dockerClient
          .stopContainerCmd(id)
          .withTimeout((int) Math.max(1, timeout.toSeconds()))
          .exec();
    } catch (Exception ignore) {
      // we can ignore this
    }
  }

  // must be removed because of withAutoRemove(true), but just in case
  private void safeRemove(String id) {
    try {
      DockerClientHolder.dockerClient.removeContainerCmd(id).withForce(true).exec();
    } catch (Exception ignore) {
      // we can ignore this
    }
  }

  private static RuntimeException mapExitCode(int exit) {
    return switch (exit) {
      case 1 -> failed("General error (exit code 1)");
      case 2 -> failed("Shell syntax error (exit code 2)");
      case 126 -> failed("Command found but not executable (exit code 126)");
      case 127 -> failed("Command not found (exit code 127)");
      case 130 -> failed("Interrupted by SIGINT (exit code 130)");
      case 137 -> failed("Killed by SIGKILL (exit code 137)");
      case 139 -> failed("Segmentation fault (exit code 139)");
      case 143 -> failed("Terminated by SIGTERM (exit code 143)");
      default -> failed("Process exited with code " + exit);
    };
  }

  private static RuntimeException failed(String message) {
    return new RuntimeException(message);
  }
}
