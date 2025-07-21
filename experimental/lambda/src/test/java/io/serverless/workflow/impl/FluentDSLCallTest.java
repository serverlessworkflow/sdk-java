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
package io.serverless.workflow.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

public class FluentDSLCallTest {

  @Test
  void testJavaFunction() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      final Workflow workflow =
          FuncWorkflowBuilder.workflow("testJavaCall")
              .tasks(tasks -> tasks.callFn(f -> f.fn(JavaFunctions::getName)))
              .build();
      assertThat(
              app.workflowDefinition(workflow)
                  .instance(new Person("Francisco", 33))
                  .start()
                  .get()
                  .asText()
                  .orElseThrow())
          .isEqualTo("Francisco Javierito");
    }
  }

  @Test
  void testForLoop() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow()
              .tasks(
                  t ->
                      t.forFn(
                          f ->
                              f.whileC(CallTest::isEven)
                                  .collection(v -> (Collection<?>) v)
                                  .tasks(CallTest::sum)))
              .build();

      assertThat(
              app.workflowDefinition(workflow)
                  .instance(List.of(2, 4, 6))
                  .start()
                  .get()
                  .asNumber()
                  .orElseThrow())
          .isEqualTo(12);
    }
  }

  @Test
  void testSwitch() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow()
              .tasks(
                  tasks ->
                      tasks
                          .switchFn(
                              switchOdd ->
                                  switchOdd.items(
                                      item ->
                                          item.when(CallTest::isOdd).then(FlowDirectiveEnum.END)))
                          .callFn(callJava -> callJava.fn(CallTest::zero)))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      assertThat(definition.instance(3).start().get().asNumber().orElseThrow()).isEqualTo(3);
      assertThat(definition.instance(4).start().get().asNumber().orElseThrow()).isEqualTo(0);
    }
  }
}
