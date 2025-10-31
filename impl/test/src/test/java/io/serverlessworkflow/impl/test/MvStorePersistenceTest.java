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
package io.serverlessworkflow.impl.test;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.persistence.PersistenceApplicationBuilder;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.bigmap.BytesMapPersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.mvstore.MVStorePersistenceStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.SAME_THREAD)
class MvStorePersistenceTest {

  @TempDir static Path tmp;

  static Path runningV1, suspendedV1, runningV0, suspendedV0;

  @BeforeAll
  static void prepareDbSamples() throws IOException, InterruptedException {
    runningV1 = tmp.resolve("running_v1.db");
    suspendedV1 = tmp.resolve("suspended_v1.db");
    runningV0 = tmp.resolve("running.db");
    suspendedV0 = tmp.resolve("suspended.db");

    ForkedDbGen.run(runningV1, false);
    ForkedDbGen.run(suspendedV1, true);
    ForkedDbGen.run(runningV0, false);
    ForkedDbGen.run(suspendedV0, true);
  }

  @Test
  void testSimpleRun() throws IOException {
    final Path dbPath = tmp.resolve("simple.db");
    try (PersistenceInstanceHandlers handlers =
            BytesMapPersistenceInstanceHandlers.builder(
                    new MVStorePersistenceStore(dbPath.toString()))
                .build();
        WorkflowApplication application =
            PersistenceApplicationBuilder.builder(WorkflowApplication.builder(), handlers.writer())
                .build()) {

      WorkflowDefinition definition =
          application.workflowDefinition(
              readWorkflowFromClasspath("workflows-samples/simple-expression.yaml"));

      assertThat(handlers.reader().readAll(definition).values()).isEmpty();

      definition.instance(Map.of()).start().join();

      assertThat(handlers.reader().readAll(definition).values()).isEmpty();
    }
  }

  @Test
  void testWaitingInstance() throws IOException {
    TaskCounterPerInstanceListener taskCounter = new TaskCounterPerInstanceListener();
    try (WorkflowApplication application =
        WorkflowApplication.builder().withListener(taskCounter).build()) {

      WorkflowDefinition definition =
          application.workflowDefinition(
              readWorkflowFromClasspath("workflows-samples/set-listen-to-any.yaml"));

      WorkflowInstance instance = definition.instance(Map.of());
      instance.start();

      assertThat(taskCounter.taskCounter(instance.id()).completed()).isEqualTo(1);
    }
  }

  @ParameterizedTest(name = "{index} ⇒ {0} should restore as {1}")
  @MethodSource("dbSamples")
  void testRestoreInstances(Path dbFile, WorkflowStatus expectedStatus) throws IOException {
    runIt(dbFile, expectedStatus);
  }

  private static Stream<Arguments> dbSamples() {
    return Stream.of(
        Arguments.of(runningV0, WorkflowStatus.WAITING),
        Arguments.of(suspendedV0, WorkflowStatus.SUSPENDED),
        Arguments.of(runningV1, WorkflowStatus.WAITING),
        Arguments.of(suspendedV1, WorkflowStatus.SUSPENDED));
  }

  private void runIt(Path dbFile, WorkflowStatus expectedStatus) throws IOException {
    TaskCounterPerInstanceListener taskCounter = new TaskCounterPerInstanceListener();

    try (PersistenceInstanceHandlers handlers =
            BytesMapPersistenceInstanceHandlers.builder(
                    new MVStorePersistenceStore(dbFile.toString()))
                .build();
        WorkflowApplication application =
            PersistenceApplicationBuilder.builder(
                    WorkflowApplication.builder()
                        .withListener(taskCounter)
                        .withListener(new TraceExecutionListener()),
                    handlers.writer())
                .build()) {

      WorkflowDefinition definition =
          application.workflowDefinition(
              readWorkflowFromClasspath("workflows-samples/set-listen-to-any.yaml"));

      Collection<WorkflowInstance> instances = handlers.reader().readAll(definition).values();

      assertThat(instances).hasSize(1);

      instances.forEach(WorkflowInstance::start);

      assertThat(instances)
          .singleElement()
          .satisfies(
              instance -> {
                assertThat(instance.status()).isEqualTo(expectedStatus);
                assertThat(taskCounter.taskCounter(instance.id()).completed()).isEqualTo(0);
              });
    }
  }
}
