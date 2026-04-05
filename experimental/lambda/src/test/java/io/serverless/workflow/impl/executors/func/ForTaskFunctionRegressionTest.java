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

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class ForTaskFunctionRegressionTest {

  @Test
  void initializesOptionalFieldsAsEmpty() {
    ForTaskFunction taskFunction = new ForTaskFunction();

    assertThat(taskFunction.getWhileClass()).isNotNull().isEmpty();
    assertThat(taskFunction.getItemClass()).isNotNull().isEmpty();
    assertThat(taskFunction.getForClass()).isNotNull().isEmpty();
  }

  @Test
  void forLoopWithoutExplicitCollectionClassExecutesSuccessfully()
      throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow("loop-without-collection-class")
              .tasks(
                  tasks ->
                      tasks.forEach(
                          f ->
                              f.whileC(CallTest::isEven)
                                  .collection(v -> (Collection<?>) v)
                                  .tasks(CallTest::sum)))
              .build();

      var result = app.workflowDefinition(workflow).instance(List.of(2, 4, 6)).start().get();

      assertThat(result.asNumber().orElseThrow()).isEqualTo(12);
    }
  }
}
