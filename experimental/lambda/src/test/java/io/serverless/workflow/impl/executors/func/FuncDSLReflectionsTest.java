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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.agent;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withContext;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.withFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContextData;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FuncDSLReflectionsTest {

  @Test
  void check_serializable_function() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("strip-function").tasks(function(String::strip)).build();
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Optional<String> output =
          app.workflowDefinition(wf).instance("Hello World!     ").start().join().asText();
      assertTrue(output.isPresent());
      assertEquals("Hello World!", output.get());
    }
  }

  @Test
  void check_serializable_function_with_non_serializable_capture() {
    // 1. Create a clearly non-serializable object
    class NonSerializableService {
      String appendSuffix(String text) {
        return text + " - Processed successfully";
      }
    }

    NonSerializableService service = new NonSerializableService();

    Workflow wf =
        FuncWorkflowBuilder.workflow("capture-function")
            .tasks(function(service::appendSuffix))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Optional<String> output =
          app.workflowDefinition(wf).instance("Test Input").start().join().asText();

      assertTrue(output.isPresent());
      assertEquals("Test Input - Processed successfully", output.get());
    }
  }

  @Test
  void check_serializable_predicate_switch() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("predicate-test")
            .tasks(
                // Infers Integer.class automatically
                switchWhenOrElse((Integer v) -> v > 10, "highValueTask", FlowDirectiveEnum.END),
                // Only executes if > 10
                function("highValueTask", (Integer v) -> v * 2))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      // Test True path
      Optional<Integer> highOutput =
          app.workflowDefinition(wf).instance(15).start().join().as(Integer.class);

      assertTrue(highOutput.isPresent());
      assertEquals(30, highOutput.get());

      // Test False path (ends immediately, returning original input)
      Optional<Integer> lowOutput =
          app.workflowDefinition(wf).instance(5).start().join().as(Integer.class);

      assertTrue(lowOutput.isPresent());
      assertEquals(5, lowOutput.get());
    }
  }

  @Test
  void check_serializable_unique_id_bifunction() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("agent-unique-id-test")
            .tasks(
                // Infers String.class for the payload (the second parameter)
                agent(
                    (String uniqueId, String payload) ->
                        "ID=[" + uniqueId + "] Payload=[" + payload + "]"))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Optional<String> output =
          app.workflowDefinition(wf).instance("Agent Data").start().join().asText();

      assertTrue(output.isPresent());
      // The uniqueId should contain the workflow instance ID and the JSON pointer
      assertTrue(output.get().contains("ID=["));
      assertTrue(output.get().contains("] Payload=[Agent Data]"));
    }
  }

  @Test
  void check_serializable_java_context_function() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("context-function-test")
            .tasks(
                // Infers String.class for the payload (the first parameter)
                withContext(
                    (String payload, WorkflowContextData wctx) ->
                        payload + " processed by " + wctx.instanceData().id()))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Optional<String> output =
          app.workflowDefinition(wf).instance("Context Data").start().join().asText();

      assertTrue(output.isPresent());
      assertTrue(output.get().startsWith("Context Data processed by "));
    }
  }

  @Test
  void check_serializable_java_filter_function() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-function-test")
            .tasks(
                // Infers String.class for the payload (the first parameter)
                withFilter(
                    (String payload, WorkflowContextData wctx, TaskContextData tctx) ->
                        payload + " at position " + tctx.position().jsonPointer()))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Optional<String> output =
          app.workflowDefinition(wf).instance("Filter Data").start().join().asText();

      assertTrue(output.isPresent());
      // It should append the task JSON pointer (likely "/tasks/0" or similar depending on spec)
      assertTrue(output.get().contains("Filter Data at position do/"));
    }
  }
}
