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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkflowNumberConversionTest {

  @Test
  void integer_score_from_task_output_is_compatible_with_outputAs_integer_class() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("numbers")
            .tasks(
                function(
                        "scoreProposal",
                        (Proposal input) -> {
                          Integer score = calculateScore(input.abstractText());
                          return score;
                        },
                        Proposal.class)
                    .outputAs(
                        (Integer score) -> new ProposalScore(score, score >= 7), Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          app.workflowDefinition(workflow)
              .instance(new Proposal("Workflow, workflow, workflow..."))
              .start()
              .join();
      Assertions.assertNotNull(model);
      ProposalScore result = model.as(ProposalScore.class).orElseThrow();
      Assertions.assertEquals(10, result.score());
      Assertions.assertTrue(result.accepted());
    }
  }

  @Test
  void long_to_integer_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("longToInt")
            .tasks(
                function("convertLong", Function.identity(), Long.class)
                    .outputAs((Integer result) -> result * 2, Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(100L).start().join();
      Integer result = model.as(Integer.class).orElseThrow();
      Assertions.assertEquals(200, result);
    }
  }

  @Test
  void integer_to_long_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("intToLong")
            .tasks(
                function("convertInt", Function.identity(), Integer.class)
                    .outputAs((Long result) -> result * 3L, Long.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(50).start().join();
      Long result = model.as(Long.class).orElseThrow();
      Assertions.assertEquals(150L, result);
    }
  }

  @Test
  void integer_to_big_integer_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("integerToBigInteger")
            .tasks(
                function("convertInt", Function.identity(), Integer.class)
                    .outputAs(
                        (BigInteger result) -> result.multiply(BigInteger.valueOf(3)),
                        BigInteger.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(50).start().join();
      BigInteger result = model.as(BigInteger.class).orElseThrow();
      Assertions.assertEquals(BigInteger.valueOf(150), result);
    }
  }

  @Test
  void double_to_integer_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("doubleToInt")
            .tasks(
                function("convertDouble", Function.identity(), Double.class)
                    .outputAs((Integer result) -> result + 5, Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(42.7).start().join();
      Integer result = model.as(Integer.class).orElseThrow();
      Assertions.assertEquals(47, result);
    }
  }

  @Test
  void double_to_big_decimal_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("doubleToInt")
            .tasks(
                function("convertDouble", Function.identity(), Double.class)
                    .outputAs(
                        (BigDecimal result) -> result.add(BigDecimal.valueOf(5)), BigDecimal.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(42.7).start().join();
      BigDecimal result = model.as(BigDecimal.class).orElseThrow();
      Assertions.assertEquals(BigDecimal.valueOf(47.7), result);
    }
  }

  @Test
  void float_to_double_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("floatToDouble")
            .tasks(
                function("convertFloat", Function.identity(), Float.class)
                    .outputAs((Double result) -> result * 1.5, Double.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(10.0f).start().join();
      Double result = model.as(Double.class).orElseThrow();
      Assertions.assertEquals(15.0, result, 0.001);
    }
  }

  @Test
  void short_to_integer_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("shortToInt")
            .tasks(
                function("convertShort", (Short input) -> input.intValue(), Short.class)
                    .outputAs((Integer result) -> result * 10, Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance((short) 5).start().join();
      Integer result = model.as(Integer.class).orElseThrow();
      Assertions.assertEquals(50, result);
    }
  }

  @Test
  void byte_to_integer_conversion() {
    Workflow workflow =
        FuncWorkflowBuilder.workflow("byteToInt")
            .tasks(
                function("convertByte", Function.identity(), Byte.class)
                    .outputAs((Integer result) -> result + 100, Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance((byte) 25).start().join();
      Integer result = model.as(Integer.class).orElseThrow();
      Assertions.assertEquals(125, result);
    }
  }

  @Test
  void number_conversion_with_string_output() {
    // This verifies that model.as(Integer.class) (via asNumber(Integer.class)) returns
    // Optional.empty()
    Workflow workflow =
        FuncWorkflowBuilder.workflow("stringOutput")
            .tasks(function("returnString", (Integer input) -> "result: " + input, Integer.class))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model = app.workflowDefinition(workflow).instance(42).start().join();
      Assertions.assertTrue(model.as(Integer.class).isEmpty());
      Assertions.assertEquals("result: 42", model.as(String.class).orElseThrow());
    }
  }

  private Integer calculateScore(String abstractText) {
    return abstractText.contains("Workflow") ? 10 : 5;
  }

  public record ProposalScore(Integer score, boolean accepted) {}

  public record Proposal(String abstractText) {}
}
