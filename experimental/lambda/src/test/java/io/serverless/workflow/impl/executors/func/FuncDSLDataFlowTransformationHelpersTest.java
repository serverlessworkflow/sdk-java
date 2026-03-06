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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.input;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.output;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class FuncDSLDataFlowTransformationHelpersTest {

  @Test
  void test_input_with_inputFrom() {

    SoftAssertions softly = new SoftAssertions();

    Workflow workflow =
        FuncWorkflowBuilder.workflow("reviewSubmissionWithModel")
            .tasks(
                function(
                    "add5",
                    (Long input) -> {
                      softly.assertThat(input).isEqualTo(10L);
                      return input + 5;
                    },
                    Long.class),
                function("returnEnriched", (Long enrichedValue) -> enrichedValue, Long.class)
                    .inputFrom(
                        (object, workflowContext) -> {
                          softly.assertThat(object).isEqualTo(15L);
                          Long input = input(workflowContext, Long.class);
                          softly.assertThat(input).isEqualTo(10L);
                          return object + input;
                        },
                        Long.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(workflow);
      WorkflowModel model = def.instance(10L).start().join();
      Number number = model.asNumber().orElseThrow();
      softly.assertThat(number.longValue()).isEqualTo(25L);
    }

    softly.assertAll();
  }

  @Test
  void test_input_with_outputAs() {

    SoftAssertions softly = new SoftAssertions();

    Workflow workflow =
        FuncWorkflowBuilder.workflow("enrichOutputWithModelTest")
            .tasks(
                function(
                        "add5",
                        (Long input) -> {
                          softly.assertThat(input).isEqualTo(10L);
                          return input + 5;
                        },
                        Long.class)
                    .outputAs(
                        (object, workflowContext, taskContextData) -> {
                          softly.assertThat(object).isEqualTo(15L);
                          Long input = input(workflowContext, Long.class);
                          softly.assertThat(input).isEqualTo(10L);
                          return input + object;
                        },
                        Long.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(workflow);

      WorkflowModel model = def.instance(10L).start().join();
      Number number = model.asNumber().orElseThrow();

      softly.assertThat(number.longValue()).isEqualTo(25L);
    }

    softly.assertAll();
  }

  @Test
  void test_output_with_exportAs() {

    SoftAssertions softly = new SoftAssertions();

    Workflow workflow =
        FuncWorkflowBuilder.workflow("enrichOutputWithInputTest")
            .tasks(
                function(
                        "add5",
                        (Long input) -> {
                          softly.assertThat(input).isEqualTo(10L);
                          return input + 5;
                        },
                        Long.class)
                    .exportAs(
                        (object, workflowContext, taskContextData) -> {
                          Long taskOutput = output(taskContextData, Long.class);
                          softly.assertThat(taskOutput).isEqualTo(15L);
                          return taskOutput * 2;
                        }))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(workflow);
      WorkflowModel model = def.instance(10L).start().join();
      Number number = model.asNumber().orElseThrow();
      softly.assertThat(number.longValue()).isEqualTo(15L);
    }

    softly.assertAll();
  }

  @Test
  void test_input_with_inputFrom_fluent_way() {
    SoftAssertions softly = new SoftAssertions();

    Workflow workflow =
        FuncWorkflowBuilder.workflow("enrichOutputWithInputTest")
            .tasks(
                function("sumFive", (Long input) -> input + 5, Long.class)
                    .inputFrom(
                        (object, workflowContext, taskContextData) ->
                            input(taskContextData, Long.class) * 2))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowDefinition def = app.workflowDefinition(workflow);
      WorkflowModel model = def.instance(10L).start().join();
      Number number = model.asNumber().orElseThrow();

      softly.assertThat(number.longValue()).isEqualTo(25L);
    }

    softly.assertAll();
  }
}
