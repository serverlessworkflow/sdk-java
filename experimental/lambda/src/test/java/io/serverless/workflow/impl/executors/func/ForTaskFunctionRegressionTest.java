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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
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
  void optionalFieldsSurviveJavaSerializationRoundTrip() throws Exception {
    ForTaskFunction taskFunction = new ForTaskFunction();
    clearField(taskFunction, "whileClass");
    clearField(taskFunction, "itemClass");
    clearField(taskFunction, "forClass");
    clearField(taskFunction, "collection");

    ForTaskFunction copy = roundTrip(taskFunction);

    assertThat(copy.getWhileClass()).isNotNull().isEmpty();
    assertThat(copy.getItemClass()).isNotNull().isEmpty();
    assertThat(copy.getForClass()).isNotNull().isEmpty();
  }

  @Test
  void forLoopWithExplicitCollectionClassExecutesSuccessfully()
      throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      ForTaskConfiguration forConfig = new ForTaskConfiguration();
      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("loop-with-collection-class")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "forLoop",
                          new Task()
                              .withForTask(
                                  new ForTaskFunction()
                                      .withWhile(CallTest::isEven)
                                      .withCollection(v -> v, Collection.class)
                                      .withFor(forConfig)
                                      .withDo(
                                          List.of(
                                              new TaskItem(
                                                  "javaCall",
                                                  new Task()
                                                      .withCallTask(
                                                          new CallTaskJava(
                                                              CallJava.loopFunction(
                                                                  CallTest::sum,
                                                                  forConfig.getEach()))))))))));

      var result = app.workflowDefinition(workflow).instance(List.of(2, 4, 6)).start().get();

      assertThat(result.asNumber().orElseThrow()).isEqualTo(12);
    }
  }

  private static ForTaskFunction roundTrip(ForTaskFunction taskFunction) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      oos.writeObject(taskFunction);
    }

    try (ObjectInputStream ois =
        new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
      return (ForTaskFunction) ois.readObject();
    }
  }

  private static void clearField(Object target, String fieldName)
      throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, null);
  }
}
