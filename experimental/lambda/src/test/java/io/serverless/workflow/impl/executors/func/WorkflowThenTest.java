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
package io.serverless.workflow.impl.executors.func;

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowThenTest {

  private static final Logger log = LoggerFactory.getLogger(WorkflowThenTest.class);

  @Test
  void consume_then_skips_next_task_and_jumps_to_target() {

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                consume("sendNewsletter", input -> log.debug("Consuming: {}", input), Object.class)
                    .then("otherTask"),
                function("nextTask", v -> "nextTask: " + v, String.class),
                function("otherTask", v -> "otherTask: " + v, String.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(wf);
      WorkflowModel model = def.instance("hello newsletter").start().join();

      String output = model.asText().orElseThrow();
      Assertions.assertEquals("otherTask: hello newsletter", output);
    }
  }

  @Test
  void function_then_skips_next_task_and_jumps_to_target() {

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                function("arrayFromString", input -> input.split(","), String.class)
                    .then("otherTask"),
                function("nextTask", arr -> "nextTask: " + Arrays.toString(arr), String[].class),
                function("otherTask", arr -> "otherTask: " + Arrays.toString(arr), String[].class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(wf);
      String output = def.instance("hello,from,cncf").start().join().asText().orElseThrow();

      Assertions.assertEquals("otherTask: [hello, from, cncf]", output);
    }
  }

  @Test
  void function_then_end_directive_stops_workflow_execution() {

    Workflow wf =
        FuncWorkflowBuilder.workflow("intelligent-newsletter")
            .tasks(
                function("uppercase", String::toUpperCase, String.class)
                    .then(FlowDirectiveEnum.END),
                function("lowercase", String::toLowerCase, String.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(wf);
      String output =
          def.instance("Hello Alice, Hello Bob, Hello Everyone!")
              .start()
              .join()
              .asText()
              .orElseThrow();

      Assertions.assertEquals("HELLO ALICE, HELLO BOB, HELLO EVERYONE!", output);
    }
  }
}
