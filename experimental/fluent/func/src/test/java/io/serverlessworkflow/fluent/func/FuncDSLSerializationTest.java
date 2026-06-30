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
package io.serverlessworkflow.fluent.func;

import static io.serverlessworkflow.api.WorkflowReader.readWorkflow;
import static io.serverlessworkflow.api.WorkflowWriter.workflowAsBytes;
import static io.serverlessworkflow.api.WorkflowWriter.workflowAsString;
import static io.serverlessworkflow.api.WorkflowWriter.writeWorkflow;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.types.Workflow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuncDSLSerializationTest {

  private static final Logger logger = LoggerFactory.getLogger(FuncDSLSerializationTest.class);

  @ParameterizedTest
  @MethodSource("workflows")
  public void testSpecFeaturesParsing(Workflow workflow) throws IOException {
    assertWorkflow(workflow);
    assertWorkflowEquals(workflow, writeAndReadInMemory(workflow));
  }

  static Stream<Workflow> workflows() {
    return Stream.of(
        FuncWorkflowBuilder.workflow("waitCompletable")
            .tasks(function(FuncDSLSerializationTest::inc))
            .build(),
        FuncWorkflowBuilder.workflow("hello")
            .tasks(t -> t.set("sayHelloWorld", b -> b.expr(Map.of("result", "hello world!"))))
            .build());
  }

  private static Workflow writeAndReadInMemory(Workflow workflow) throws IOException {
    byte[] bytes;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeWorkflow(out, workflow, WorkflowFormat.JSON);
      bytes = out.toByteArray();
    }
    logger.debug("Serialized json is " + new String(bytes));
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      return readWorkflow(in, WorkflowFormat.JSON);
    }
  }

  private static void assertWorkflow(Workflow workflow) {
    assertNotNull(workflow);
    assertNotNull(workflow.getDocument());
    assertNotNull(workflow.getDo());
  }

  private static void assertWorkflowEquals(Workflow workflow, Workflow other) throws IOException {
    assertThat(workflowAsString(workflow, WorkflowFormat.YAML))
        .isEqualTo(workflowAsString(other, WorkflowFormat.YAML));
    assertThat(workflowAsBytes(workflow, WorkflowFormat.JSON))
        .isEqualTo(workflowAsBytes(other, WorkflowFormat.JSON));
  }

  private static Integer inc(Integer quantity) {
    return quantity++;
  }

  //  private static class MyFunction implements SerializableFunction<Integer, Integer> {
  //
  //    private static final long serialVersionUID = 1L;
  //
  //    @Override
  //    public Integer apply(Integer arg0) {
  //      return arg0++;
  //    }
  //  }
  //
  //  public static void main(String[] args) throws JsonProcessingException {
  //    System.out.println(
  //        WorkflowFormat.JSON
  //            .mapper()
  //            .writeValueAsString(
  //                new CallJava.CallJavaFunction<Integer, Integer>(
  //                    FuncDSLSerializationTest::inc,
  //                    Optional.of(Integer.class),
  //                    Optional.of(Integer.class))));
  //
  //    System.out.println(
  //        WorkflowFormat.JSON
  //            .mapper()
  //            .writeValueAsString(
  //                new CallJava.CallJavaFunction<Integer, Integer>(
  //                    new MyFunction(), Optional.of(Integer.class), Optional.of(Integer.class))));
  //  }
}
