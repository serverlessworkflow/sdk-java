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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.spec.WorkflowBuilder;
import io.serverlessworkflow.fluent.spec.dsl.DSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class WaitTest {

  @Test
  void waitTaskBuilder_should_wait_5_seconds() {
    Workflow workflow =
        WorkflowBuilder.workflow("waitTaskBuilder", "wait")
            .tasks(
                t ->
                    t.wait(
                            "wait5Seconds",
                            w -> w.wait(timeout -> timeout.duration(d -> d.seconds(5))))
                        .set("setFinished", s -> s.put("finished", true)))
            .build();

    assertThatExecuteAtLeast5Seconds(workflow);
  }

  @Test
  void durationExpressions_should_wait_5_seconds() {
    Workflow workflow = WorkflowBuilder.workflow("durationExpr", "wait")
            .tasks(DSL.wait("PT5S"), DSL.set("setFinished", s -> s.put("finished", true)))
            .build();

    assertThatExecuteAtLeast5Seconds(workflow);
  }

  @Test
  void namedDurationExpressions_should_wait_5_seconds() {
    Workflow workflow = WorkflowBuilder.workflow("durationExpr", "wait")
            .tasks(DSL.wait("named","PT5S"), DSL.set("setFinished", s -> s.put("finished", true)))
            .build();

    assertThatExecuteAtLeast5Seconds(workflow);
  }

  @Test
  void timeoutBuilder_should_wait_5_seconds() {
    Workflow workflow = WorkflowBuilder.workflow("durationExpr", "wait")
            .tasks(DSL.wait(timeoutBuilder -> timeoutBuilder.duration(durationInlineBuilder -> durationInlineBuilder.seconds(5))), DSL.set("setFinished", s -> s.put("finished", true)))
            .build();

    assertThatExecuteAtLeast5Seconds(workflow);
  }

  private static void assertThatExecuteAtLeast5Seconds(Workflow workflow) {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition definition = app.workflowDefinition(workflow);
      long startNanos = System.nanoTime();
      WorkflowModel model = definition.instance(Map.of()).start().join();
      long elapsedMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

      assertEquals(true, model.asMap().orElseThrow().get("finished"));
      assertTrue(
              elapsedMillis >= 5_000,
              "Workflow should wait at least 5 seconds, but waited " + elapsedMillis + " ms");
    }
  }
}
