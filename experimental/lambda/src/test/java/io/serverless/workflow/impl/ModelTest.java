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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.DurationInline;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.api.types.WaitTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.OutputAsFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class ModelTest {

  @Test
  void testStringExpression() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testString").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "doNothing",
                          new Task()
                              .withWaitTask(
                                  new WaitTask()
                                      .withWait(
                                          new TimeoutAfter()
                                              .withDurationInline(
                                                  new DurationInline().withMilliseconds(10)))))))
              .withOutput(
                  new Output()
                      .withAs(
                          new OutputAsFunction().withFunction(JavaFunctions::addJavieritoString)));

      assertThat(
              app.workflowDefinition(workflow)
                  .instance("Francisco")
                  .start()
                  .get()
                  .asText()
                  .orElseThrow())
          .isEqualTo("Francisco Javierito");
    }
  }

  @Test
  void testMapExpression() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testMap").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "javierito",
                          new Task()
                              .withSetTask(
                                  new SetTask()
                                      .withSet(
                                          new Set()
                                              .withSetTaskConfiguration(
                                                  new SetTaskConfiguration()
                                                      .withAdditionalProperty("name", "Francisco")))
                                      .withOutput(
                                          new Output()
                                              .withAs(
                                                  new OutputAsFunction()
                                                      .withFunction(
                                                          JavaFunctions::addJavierito)))))));
      assertThat(
              app.workflowDefinition(workflow)
                  .instance(Map.of())
                  .start()
                  .get()
                  .asMap()
                  .map(m -> m.get("name"))
                  .orElseThrow())
          .isEqualTo("Francisco Javierito");
    }
  }

  @Test
  void testStringPOJOExpression() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testPojo").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "doNothing",
                          new Task()
                              .withWaitTask(
                                  new WaitTask()
                                      .withWait(
                                          new TimeoutAfter()
                                              .withDurationInline(
                                                  new DurationInline().withMilliseconds(10)))))))
              .withOutput(
                  new Output()
                      .withAs(new OutputAsFunction().withFunction(JavaFunctions::personPojo)));

      assertThat(
              app.workflowDefinition(workflow)
                  .instance("Francisco")
                  .start()
                  .get()
                  .as(Person.class)
                  .orElseThrow()
                  .name())
          .isEqualTo("Francisco Javierito");
    }
  }

  @Test
  void testPOJOStringExpression() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testPojo").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "doNothing",
                          new Task()
                              .withWaitTask(
                                  new WaitTask()
                                      .withWait(
                                          new TimeoutAfter()
                                              .withDurationInline(
                                                  new DurationInline().withMilliseconds(10)))))))
              .withOutput(
                  new Output().withAs(new OutputAsFunction().withFunction(JavaFunctions::getName)));

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
  void testPOJOStringExpressionWithContext() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testPojo").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "doNothing",
                          new Task()
                              .withWaitTask(
                                  new WaitTask()
                                      .withWait(
                                          new TimeoutAfter()
                                              .withDurationInline(
                                                  new DurationInline().withMilliseconds(10)))))))
              .withOutput(
                  new Output()
                      .withAs(new OutputAsFunction().withFunction(JavaFunctions::getContextName)));
      WorkflowInstance instance =
          app.workflowDefinition(workflow).instance(new Person("Francisco", 33));
      assertThat(instance.start().get().asText().orElseThrow())
          .isEqualTo("Francisco_" + instance.id());
    }
  }

  @Test
  void testPOJOStringExpressionWithFilter() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document().withNamespace("test").withName("testPojo").withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "doNothing",
                          new Task()
                              .withWaitTask(
                                  new WaitTask()
                                      .withOutput(
                                          new Output()
                                              .withAs(
                                                  new OutputAsFunction()
                                                      .withFunction(JavaFunctions::getFilterName)))
                                      .withWait(
                                          new TimeoutAfter()
                                              .withDurationInline(
                                                  new DurationInline().withMilliseconds(10)))))));
      WorkflowInstance instance =
          app.workflowDefinition(workflow).instance(new Person("Francisco", 33));
      assertThat(instance.start().get().asText().orElseThrow())
          .isEqualTo("Francisco_" + instance.id() + "_doNothing");
    }
  }
}
