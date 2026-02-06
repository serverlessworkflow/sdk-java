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
import io.serverlessworkflow.impl.persistence.DefaultPersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.PersistenceApplicationBuilder;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.mvstore.MVStorePersistenceStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class MvStorePersistenceTest {

  @Test
  void testSimpleRun() throws Exception {
    final String dbName = "db-samples/simple.db";
    try (PersistenceInstanceHandlers handlers =
            DefaultPersistenceInstanceHandlers.builder(new MVStorePersistenceStore(dbName))
                .withExecutorService(Executors.newSingleThreadExecutor())
                .withCloseTimeout(Duration.ofMillis(100))
                .build();
        WorkflowApplication application =
            PersistenceApplicationBuilder.builder(WorkflowApplication.builder(), handlers.writer())
                .build(); ) {
      WorkflowDefinition definition =
          application.workflowDefinition(
              readWorkflowFromClasspath("workflows-samples/simple-expression.yaml"));
      assertNoInstance(handlers, definition);
      definition.instance(Map.of()).start().join();
      handlers.writer().close();
      assertNoInstance(handlers, definition);
    } finally {
      Files.delete(Path.of(dbName));
    }
  }

  private void assertNoInstance(
      PersistenceInstanceHandlers handlers, WorkflowDefinition definition) {
    try (Stream<WorkflowInstance> stream = handlers.reader().scanAll(definition)) {
      assertThat(stream.count()).isEqualTo(0);
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

  @Test
  void testRestoreWaitingInstanceV0() throws IOException {
    runIt("db-samples/running.db", WorkflowStatus.WAITING);
  }

  @Test
  void testRestoreSuspendedInstanceV0() throws IOException {
    runIt("db-samples/suspended.db", WorkflowStatus.SUSPENDED);
  }

  @Test
  void testRestoreWaitingInstanceV1() throws IOException {
    runIt("db-samples/running_v1.db", WorkflowStatus.WAITING);
  }

  @Test
  void testRestoreSuspendedInstanceV1() throws IOException {
    runIt("db-samples/suspended_v1.db", WorkflowStatus.SUSPENDED);
  }

  private void runIt(String dbName, WorkflowStatus expectedStatus) throws IOException {
    TaskCounterPerInstanceListener taskCounter = new TaskCounterPerInstanceListener();
    try (PersistenceInstanceHandlers handlers =
            DefaultPersistenceInstanceHandlers.from(new MVStorePersistenceStore(dbName));
        WorkflowApplication application =
            PersistenceApplicationBuilder.builder(
                    WorkflowApplication.builder()
                        .withListener(taskCounter)
                        .withListener(new TraceExecutionListener()),
                    handlers.writer())
                .build(); ) {
      WorkflowDefinition definition =
          application.workflowDefinition(
              readWorkflowFromClasspath("workflows-samples/set-listen-to-any.yaml"));

      try (Stream<WorkflowInstance> stream = handlers.reader().scanAll(definition)) {
        Collection<WorkflowInstance> instances = stream.toList();
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
}
