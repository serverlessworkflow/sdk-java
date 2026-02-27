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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkflowThenTest {

  @Test
  void consume_then_skips_next_task_and_jumps_to_target() {
    AtomicInteger a = new AtomicInteger();
    AtomicInteger b = new AtomicInteger();

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                consume("sendNewsletter", (String s) -> {}, String.class).then("otherTask"),
                function(
                    "nextTask",
                    (v) -> {
                      a.incrementAndGet();
                      return v.strip();
                    },
                    String.class),
                function(
                    "otherTask",
                    (v) -> {
                      b.incrementAndGet();
                      return v.strip();
                    },
                    String.class))
            .build();

    WorkflowApplication app = WorkflowApplication.builder().build();
    WorkflowDefinition def = app.workflowDefinition(wf);
    def.instance(Map.of()).start().join();

    Assertions.assertEquals(1, b.get(), "otherTask should execute");
    Assertions.assertEquals(0, a.get(), "nextTask should not execute");
  }

  @Test
  void function_then_skips_next_task_and_jumps_to_target() {
    AtomicInteger a = new AtomicInteger();
    AtomicInteger b = new AtomicInteger();

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                function("myfunction", String::trim, String.class).then("otherTask"),
                function(
                    "nextTask",
                    (v) -> {
                      a.incrementAndGet();
                      return v.strip();
                    },
                    String.class),
                function(
                    "otherTask",
                    (v) -> {
                      b.incrementAndGet();
                      return v.strip();
                    },
                    String.class))
            .build();

    WorkflowApplication app = WorkflowApplication.builder().build();
    WorkflowDefinition def = app.workflowDefinition(wf);
    def.instance(Map.of()).start().join();

    Assertions.assertEquals(1, b.get(), "otherTask should execute");
    Assertions.assertEquals(0, a.get(), "nextTask should not execute");
  }

  @Test
  void function_then_end_directive_stops_workflow_execution() {
    AtomicInteger a = new AtomicInteger();

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                function("myfunction", String::trim, String.class).then(FlowDirectiveEnum.END),
                function(
                    "nextTask",
                    (v) -> {
                      a.incrementAndGet();
                      return v.strip();
                    },
                    String.class))
            .build();

    WorkflowApplication app = WorkflowApplication.builder().build();
    WorkflowDefinition def = app.workflowDefinition(wf);
    def.instance(Map.of()).start().join();

    Assertions.assertEquals(0, a.get(), "nextTask should not execute when then(END) is set");
  }
}
