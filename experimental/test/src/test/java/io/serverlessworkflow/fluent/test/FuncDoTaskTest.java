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

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class FuncDoTaskTest {

  @Test
  void testDoTaskRaiseAndTryCatch() throws Exception {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      var workflow =
          FuncWorkflowBuilder.workflow("test-do-task")
              .tasks(
                  doTask ->
                      doTask.tryCatch(
                          "try-catch-task",
                          tryBuilder ->
                              tryBuilder
                                  .tryHandler(
                                      tryBlock ->
                                          tryBlock.raise(
                                              "raise-error-task",
                                              raiseBlock ->
                                                  raiseBlock.error(
                                                      error ->
                                                          error
                                                              .type(
                                                                  URI.create(
                                                                      "http://example.com/error"))
                                                              .status(500))))
                                  .catchHandler(
                                      catchBlock ->
                                          catchBlock
                                              .errorsWith(
                                                  errs -> errs.type("http://example.com/error"))
                                              .doTasks(
                                                  catchTasks ->
                                                      catchTasks.set(
                                                          "catchHandled",
                                                          setBlock ->
                                                              setBlock.expr(
                                                                  Map.of("handled", true)))))))
              .build();

      WorkflowInstance instance = app.workflowDefinition(workflow).instance(Map.of());

      CompletableFuture<WorkflowModel> future = instance.start();
      WorkflowModel result = future.join();

      assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
      assertThat(result.as(Map.class).orElseThrow())
          .containsEntry("handled", true)
          .containsKey("handled");
    }
  }
}
