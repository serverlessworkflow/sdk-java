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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.NameParser;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
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

    try {
      var resolver = new StringExpressionResolver(workflowContext, taskContext, input);
      applyPropertySetters(resolver);
      pullImageIfNeeded(container.getImage());

      String id = createAndStartContainer();
      int exit = waitAccordingToLifetime(id, workflowContext, taskContext, input);

      return mapExitCode(exit, input);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return failed("Interrupted while waiting for container");
    } catch (Exception e) {
      return failed("Container run failed: " + e.getMessage());
    }
  }

  private void applyPropertySetters(StringExpressionResolver resolver) {
    for (var setter : propertySetters) setter.accept(resolver);
  }

  private void pullImageIfNeeded(String imageRef) throws InterruptedException {
    NameParser.ReposTag rt = NameParser.parseRepositoryTag(imageRef);
    NameParser.HostnameReposName hr = NameParser.resolveRepositoryName(imageRef);

    String repository = hr.reposName;
    String tag = rt.tag != null && rt.tag.isEmpty() ? rt.tag : "latest";
    dockerClient
        .pullImageCmd(repository)
        .withTag(tag)
        .exec(new PullImageResultCallback())
        .awaitCompletion();
  }

  private String createAndStartContainer() {
    CreateContainerResponse resp = createContainerCmd.exec();
    String id = resp.getId();
    if (id == null || id.isEmpty()) {
      throw new IllegalStateException("Container creation failed: empty ID");
    }
    dockerClient.startContainerCmd(id).exec();
    return id;
  }

  private int waitAccordingToLifetime(
      String id, WorkflowContext workflowContext, TaskContext taskContext, WorkflowModel input)
      throws Exception {

    var lifetime = container.getLifetime();
    var policy = lifetime != null ? lifetime.getCleanup() : null;

    try (var cb = dockerClient.waitContainerCmd(id).exec(new WaitContainerResultCallback())) {

      if (policy == ContainerCleanupPolicy.EVENTUALLY) {
        Duration timeout = resolveAfter(lifetime, workflowContext, taskContext, input);
        int exit = cb.awaitStatusCode(timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (isRunning(id)) {
          safeStop(id, Duration.ofSeconds(10));
        }
        safeRemove(id);
        return exit;

      } else {
        int exit = cb.awaitStatusCode();
        if (policy == ContainerCleanupPolicy.ALWAYS) {
          safeRemove(id);
        }
        return exit;
      }
    }
  }

  private Duration resolveAfter(
      io.serverlessworkflow.api.types.ContainerLifetime lifetime,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {

    if (lifetime == null || lifetime.getAfter() == null) {
      return Duration.ZERO;
    }
    WorkflowValueResolver<Duration> r =
        WorkflowUtils.fromTimeoutAfter(definition.application(), lifetime.getAfter());
    return r.apply(workflowContext, taskContext, input);
  }

  private boolean isRunning(String id) {
    try {
      var st = dockerClient.inspectContainerCmd(id).exec().getState();
      return st != null && Boolean.TRUE.equals(st.getRunning());
    } catch (Exception e) {
      return false; // must be already removed
    }
  }

  private void safeStop(String id, Duration timeout) {
    try {
      dockerClient.stopContainerCmd(id).withTimeout((int) Math.max(1, timeout.toSeconds())).exec();
    } catch (Exception ignore) {
      // we can ignore this
    }
  }

  // must be removed because of withAutoRemove(true), but just in case
  private void safeRemove(String id) {
    try {
      dockerClient.removeContainerCmd(id).withForce(true).exec();
    } catch (Exception ignore) {
      // we can ignore this
    }
  }

  private static <T> CompletableFuture<T> mapExitCode(int exit, T ok) {
    return switch (exit) {
      case 0 -> CompletableFuture.completedFuture(ok);
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
