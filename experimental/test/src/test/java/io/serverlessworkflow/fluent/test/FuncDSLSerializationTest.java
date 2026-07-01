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
package io.serverlessworkflow.fluent.test;

import static io.serverlessworkflow.api.WorkflowWriter.workflowAsBytes;
import static io.serverlessworkflow.api.WorkflowWriter.workflowAsString;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withContext;
import static io.serverlessworkflow.fluent.test.TestSerializationUtils.writeAndReadInMemory;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FuncDSLSerializationTest {

  @ParameterizedTest
  @MethodSource("workflows")
  public void testSpecFeaturesParsing(Workflow workflow, Consumer<WorkflowDefinition> assertion)
      throws IOException {
    Workflow otherWorkflow = writeAndReadInMemory(workflow);
    assertWorkflowEquals(workflow, otherWorkflow);
    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      assertion.accept(application.workflowDefinition(otherWorkflow));
    }
  }

  static Stream<Arguments> workflows() {
    final int QUANTITY = 3;
    return Stream.of(
        Arguments.of(
            FuncWorkflowBuilder.workflow("hello")
                .tasks(t -> t.set("sayHelloWorld", b -> b.expr(Map.of("result", "hello world!"))))
                .build(),
            new CheckResult(Map.of(), Map.of("result", "hello world!"))),
        Arguments.of(
            FuncWorkflowBuilder.workflow("inc")
                .tasks(function(FuncDSLSerializationTest::inc))
                .build(),
            new CheckResult(1, 2)),
        Arguments.of(
            FuncWorkflowBuilder.workflow("incContext")
                .tasks(withContext(FuncDSLSerializationTest::incContext))
                .build(),
            new CheckResult(1, 3)),
        Arguments.of(
            FuncWorkflowBuilder.workflow("incLambda")
                .tasks(function((Integer number) -> number + QUANTITY))
                .build(),
            new CheckResult(1, 4)));
  }

  private static class CheckResult implements Consumer<WorkflowDefinition> {

    private final Object input;
    private final Object output;

    public CheckResult(Object input, Object output) {
      this.input = input;
      this.output = output;
    }

    @Override
    public void accept(WorkflowDefinition t) {
      assertThat(t.instance(this.input).start().join().asJavaObject()).isEqualTo(this.output);
    }
  }

  private static void assertWorkflowEquals(Workflow workflow, Workflow other) throws IOException {
    assertThat(workflowAsString(workflow, WorkflowFormat.YAML))
        .isEqualTo(workflowAsString(other, WorkflowFormat.YAML));
    assertThat(workflowAsBytes(workflow, WorkflowFormat.JSON))
        .isEqualTo(workflowAsBytes(other, WorkflowFormat.JSON));
  }

  private static Integer inc(Integer quantity) {
    return quantity + 1;
  }

  private static Integer incContext(Integer quantity, WorkflowContextData workflowContext) {
    return quantity + 2;
  }
}
