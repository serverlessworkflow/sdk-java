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

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.utils.ForTaskFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class ForTaskFunctionRegressionTest {

  @Test
  void forLoopWithExplicitCollectionClassExecutesSuccessfully()
      throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      ForTaskConfiguration forConfig = new ForTaskConfiguration();
      ForTask forTask =
          new ForTask()
              .withDo(
                  List.of(
                      new TaskItem(
                          "javaCall",
                          new Task()
                              .withCallTask(
                                  new CallTask()
                                      .withCallFunction(
                                          CallJava.loopFunction(
                                              CallTest::sum, forConfig.getEach()))))));
      ForTaskFunction.withWhile(forTask, CallTest::isEven);
      ForTaskFunction.withCollection(forTask, v -> v, Collection.class);
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("loop-with-collection-class")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem("forLoop", new Task().withForTask(forTask.withFor(forConfig)))));

      var result = app.workflowDefinition(workflow).instance(List.of(2, 4, 6)).start().get();

      assertThat(result.asNumber().orElseThrow()).isEqualTo(12);
    }
  }
}
