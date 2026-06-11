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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.fluent.spec.dsl.DSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WaitExecutorTest {

  private static WorkflowApplication appl;

  @BeforeAll
  static void init() {
    appl = WorkflowApplication.builder().build();
  }

  @AfterAll
  static void tearDown() {
    appl.close();
  }

  // ========== DurationInline Tests ==========

  @Test
  void testWaitWithDurationInlineSeconds() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-inline-seconds", "test", "0.1.0")
            .tasks(DSL.waitSeconds(1))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1000);
  }

  @Test
  void testWaitWithDurationInlineMilliseconds() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-inline-millis", "test", "0.1.0")
            .tasks(DSL.waitMillis(100))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(100);
  }

  @Test
  void testWaitWithDurationInlineComposite() {
    // Test composite duration with multiple components
    Workflow workflow =
        WorkflowBuilder.workflow("wait-inline-composite", "test", "0.1.0")
            .tasks(DSL.wait(Duration.ofSeconds(1).plusMillis(500)))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1500); // 1 second + 500 milliseconds
  }

  // ========== DurationLiteral Tests (TimeoutAfter.durationLiteral) ==========

  @Test
  void testWaitWithDurationLiteralISO8601Seconds() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-literal-seconds", "test", "0.1.0")
            .tasks(
                list ->
                    list.wait(
                        w ->
                            w.build()
                                .setWait(
                                    new io.serverlessworkflow.api.types.TimeoutAfter()
                                        .withDurationLiteral("PT1S"))))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1000);
  }
        WorkflowBuilder.workflow("wait-literal-seconds", "test", "0.1.0")
            .tasks(DSL.wait(Duration.parse("PT1S")))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1000);
  }

  @Test
  void testWaitWithDurationLiteralISO8601Composite() {
    // PT1.5S = 1 second 500 milliseconds (keep test fast)
    Workflow workflow =
        WorkflowBuilder.workflow("wait-literal-composite", "test", "0.1.0")
            .tasks(
                list ->
                    list.wait(
                        w ->
                            w.build()
                                .setWait(
                                    new io.serverlessworkflow.api.types.TimeoutAfter()
                                        .withDurationLiteral("PT1.5S"))))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1500);
  }

  @Test
  void testWaitWithDurationLiteralISO8601Milliseconds() {
    // PT0.1S = 100 milliseconds
    Workflow workflow =
        WorkflowBuilder.workflow("wait-literal-millis", "test", "0.1.0")
            .tasks(
                list ->
                    list.wait(
                        w ->
                            w.build()
                                .setWait(
                                    new io.serverlessworkflow.api.types.TimeoutAfter()
                                        .withDurationLiteral("PT0.1S"))))
            .build();

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(100);
  }

  // ========== DurationExpression Tests (via YAML) ==========

  @Test
  void testWaitWithDurationExpressionFromInput() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/wait-expression-input.yaml");

    long startTime = System.currentTimeMillis();
    WorkflowModel model =
        appl.workflowDefinition(workflow).instance(Map.of("timeout", "PT1S")).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(1000);
  }

  @Test
  void testWaitWithDurationExpressionFromContext() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/wait-expression-context.yaml");

    long startTime = System.currentTimeMillis();
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    long elapsed = System.currentTimeMillis() - startTime;

    assertThat(model).isNotNull();
    assertThat(elapsed).isGreaterThanOrEqualTo(500);
  }

  @Test
  void testWaitWithDurationExpressionInvalidValue() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/wait-expression-input.yaml");

    assertThatThrownBy(
            () ->
                appl.workflowDefinition(workflow)
                    .instance(Map.of("timeout", "not-a-duration"))
                    .start()
                    .join())
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid ISO 8601 duration");
  }

  @Test
  void testWaitWithDurationExpressionMissingValue() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/wait-expression-input.yaml");

    // When the expression resolves to empty/null, we throw IllegalArgumentException with helpful
    // message
    assertThatThrownBy(() -> appl.workflowDefinition(workflow).instance(Map.of()).start().join())
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("evaluated to empty or null");
  }

  // ========== Workflow Status Tests ==========

  @Test
  void testWaitSetsWorkflowStatusToWaiting() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-status-waiting", "test", "0.1.0")
            .tasks(DSL.waitMillis(500))
            .build();

    WorkflowInstance instance = appl.workflowDefinition(workflow).instance(Map.of());
    CompletableFuture<WorkflowModel> future = instance.start();

    await()
        .pollDelay(Duration.ofMillis(5))
        .atMost(Duration.ofMillis(100))
        .until(() -> instance.status() == WorkflowStatus.WAITING);

    assertThat(instance.status()).isEqualTo(WorkflowStatus.WAITING);

    future.join();
    assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
  }

  @Test
  void testWaitWithSuspendAndResume() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-suspend-resume", "test", "0.1.0")
            .tasks(DSL.waitMillis(500))
            .build();

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
    assertThat(model).isNotNull();
  }

  // ========== YAML Sample Test ==========

  @Test
  void testWaitFromYamlSample() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("workflows-samples/wait-set.yaml");
    WorkflowModel model = appl.workflowDefinition(workflow).instance(Map.of()).start().join();
    assertThat(model).isNotNull();
  }

  // ========== Convenience Methods Tests ==========

  @Test
  void testWaitSecondsConvenienceMethod() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-convenience-seconds", "test", "0.1.0")
            .tasks(DSL.waitSeconds(1))
            .build();

    var waitTask = workflow.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();
    assertThat(waitTask.getWait().getDurationInline().getSeconds()).isEqualTo(1);
  }

  @Test
  void testWaitMillisConvenienceMethod() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-convenience-millis", "test", "0.1.0")
            .tasks(DSL.waitMillis(100))
            .build();

    var waitTask = workflow.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();
    assertThat(waitTask.getWait().getDurationInline().getMilliseconds()).isEqualTo(100);
  }

  @Test
  void testWaitWithJavaDurationConvenienceMethod() {
    Workflow workflow =
        WorkflowBuilder.workflow("wait-convenience-duration", "test", "0.1.0")
            .tasks(DSL.wait(Duration.ofSeconds(1).plusMillis(500)))
            .build();

    var waitTask = workflow.getDo().get(0).getTask().getWaitTask();
    assertThat(waitTask).isNotNull();
    var inline = waitTask.getWait().getDurationInline();
    assertThat(inline.getSeconds()).isEqualTo(1);
    assertThat(inline.getMilliseconds()).isEqualTo(500);
  }
}
