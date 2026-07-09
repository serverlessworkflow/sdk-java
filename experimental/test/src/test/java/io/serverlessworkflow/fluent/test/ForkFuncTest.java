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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ForkFuncTest {

  private int value = 2;

  public int getValue() {
    return value;
  }

  @Test
  void testForkVerbose() throws IOException {
    testIt(
        FuncWorkflowBuilder.workflow("parallel-execution-workflow")
            .tasks(
                funcTaskItemListBuilder ->
                    funcTaskItemListBuilder.fork(
                        funcForkTaskBuilder ->
                            funcForkTaskBuilder.branches(
                                inner -> {
                                  inner.function(f -> f.function(this::doubleIt));
                                  inner.function(f -> f.function(this::halfIt));
                                })))
            .build());
  }

  @Test
  void testForkSyntaxSugar() throws IOException {
    testIt(
        FuncWorkflowBuilder.workflow("parallel-execution-workflow")
            .tasks(fork(function(this::doubleIt), function(this::halfIt)))
            .build());
  }

  private void testIt(Workflow workflow) throws IOException {
    workflow = TestSerializationUtils.writeAndReadInMemory(workflow);
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      assertThat(
              app.workflowDefinition(workflow).instance(8).start().join().asCollection().stream()
                  .flatMap(m -> m.asMap().orElseThrow().values().stream())
                  .toList())
          .containsExactlyInAnyOrder(2, 18);
    }
  }

  private int doubleIt(int number) {
    return (number << 1) + value;
  }

  private int halfIt(int number) {
    return (number >> 1) - value;
  }
}
