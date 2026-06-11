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
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.tasks;
import static io.serverlessworkflow.fluent.spec.dsl.DSL.timeoutSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.dsl.FuncDSL;
import org.junit.jupiter.api.Test;

public class FuncDSLWaitTest {

  @Test
  public void when_wait_with_string_expression() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.wait("PT5S"))).build();

    assertEquals(1, wf.getDo().size());
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertNotNull(waitTask);
    assertEquals("PT5S", waitTask.getWait().get());
  }

  @Test
  public void when_wait_with_timeout_builder() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait(timeoutSeconds(10))))
            .build();

    assertEquals(1, wf.getDo().size());
    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    assertNotNull(waitTask);
    assertEquals(10, waitTask.getWait().getDurationInline().getSeconds());
  }

  @Test
  public void when_wait_named_with_string() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait("pause", "PT15S")))
            .build();

    assertEquals("pause", wf.getDo().get(0).getName());
    assertEquals("PT15S", wf.getDo().get(0).getTask().getWaitTask().getWait().get());
  }

  @Test
  public void when_wait_seconds_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.waitSeconds(30))).build();

    var waitTask = wf.getDo().get(0).getTask().getWaitTask();
    var inline = waitTask.getWait().getDurationInline();
    assertNotNull(inline);
    assertEquals(30, inline.getSeconds());
    assertEquals(0, inline.getMinutes());
  }

  @Test
  public void when_wait_seconds_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitSeconds("pause", 45)))
            .build();

    assertEquals("pause", wf.getDo().get(0).getName());
    assertEquals(
        45, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getSeconds());
  }

  @Test
  public void when_wait_minutes_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.waitMinutes(10))).build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertEquals(10, inline.getMinutes());
    assertEquals(0, inline.getSeconds());
  }

  @Test
  public void when_wait_minutes_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMinutes("delay", 15)))
            .build();

    assertEquals("delay", wf.getDo().get(0).getName());
    assertEquals(
        15, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getMinutes());
  }

  @Test
  public void when_wait_hours_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.waitHours(2))).build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertEquals(2, inline.getHours());
  }

  @Test
  public void when_wait_hours_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitHours("longPause", 3)))
            .build();

    assertEquals("longPause", wf.getDo().get(0).getName());
    assertEquals(
        3, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getHours());
  }

  @Test
  public void when_wait_days_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.waitDays(1))).build();

    assertEquals(
        1, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getDays());
  }

  @Test
  public void when_wait_days_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitDays("dailyDelay", 5)))
            .build();

    assertEquals("dailyDelay", wf.getDo().get(0).getName());
    assertEquals(
        5, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getDays());
  }

  @Test
  public void when_wait_millis_unnamed() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow").tasks(tasks(FuncDSL.waitMillis(500))).build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertEquals(500, inline.getMilliseconds());
  }

  @Test
  public void when_wait_millis_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.waitMillis("shortPause", 250)))
            .build();

    assertEquals("shortPause", wf.getDo().get(0).getName());
    assertEquals(
        250,
        wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getMilliseconds());
  }

  @Test
  public void when_wait_with_duration() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait(java.time.Duration.ofMinutes(5).plusSeconds(30))))
            .build();

    var inline = wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline();
    assertEquals(5, inline.getMinutes());
    assertEquals(30, inline.getSeconds());
  }

  @Test
  public void when_wait_with_duration_named() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("waitFlow")
            .tasks(tasks(FuncDSL.wait("custom", java.time.Duration.ofHours(1))))
            .build();

    assertEquals("custom", wf.getDo().get(0).getName());
    assertEquals(
        1, wf.getDo().get(0).getTask().getWaitTask().getWait().getDurationInline().getHours());
  }
}
