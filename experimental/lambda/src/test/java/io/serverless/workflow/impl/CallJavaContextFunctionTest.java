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
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.api.types.func.JavaContextFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class CallJavaContextFunctionTest {

  // Reuse the same Person type used in CallTest
  record Person(String name, int age) {}

  @Test
  void testJavaContextFunction_simple() throws InterruptedException, ExecutionException {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      var ctxFn =
          (JavaContextFunction<Person, String>)
              (person, workflowContext) ->
                  person.name
                      + "@"
                      + workflowContext.definition().workflow().getDocument().getName();

      Workflow workflow =
          new Workflow()
              .withDocument(
                  new Document()
                      .withNamespace("test")
                      .withName("testJavaContextCall")
                      .withVersion("1.0"))
              .withDo(
                  List.of(
                      new TaskItem(
                          "javaContextCall",
                          new Task()
                              .withCallTask(
                                  new CallTaskJava(CallJava.function(ctxFn, Person.class))))));

      var out =
          app.workflowDefinition(workflow)
              .instance(new Person("Elisa", 30))
              .start()
              .get()
              .asText()
              .orElseThrow();

      assertThat(out).isEqualTo("Elisa@testJavaContextCall");
    }
  }
}
