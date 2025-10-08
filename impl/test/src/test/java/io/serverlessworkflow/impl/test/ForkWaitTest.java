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
import static org.awaitility.Awaitility.await;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ForkWaitTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() throws IOException {
    appl = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void tearDown() throws IOException {
    appl.close();
  }

  @Test
  void testForkWait() throws IOException, InterruptedException, ExecutionException {
    assertModel(
        appl.workflowDefinition(readWorkflowFromClasspath("workflows-samples/fork-wait.yaml"))
            .instance(Map.of())
            .start()
            .join());
  }

  @Test
  void testForkWaitWithSuspend() throws IOException, InterruptedException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/fork-wait.yaml");
    WorkflowInstance instance = appl.workflowDefinition(workflow).instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();
    await()
        .pollDelay(Duration.ofMillis(5))
        .atMost(Duration.ofMillis(100))
        .until(() -> instance.status() == WorkflowStatus.WAITING);
    instance.suspend();
    assertThat(instance.status()).isEqualTo(WorkflowStatus.SUSPENDED);
    instance.resume();
    WorkflowModel model = future.join();
    assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    assertModel(model);
  }

  private void assertModel(WorkflowModel current) {
    assertThat((Collection<Map<String, Object>>) current.asJavaObject())
        .containsExactlyInAnyOrderElementsOf(
            List.of(
                Map.of("helloBranch", Map.of("value", 1)),
                Map.of("byeBranch", Map.of("value", 2))));
  }
}
