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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;
import static org.junit.jupiter.api.Assertions.*;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import org.junit.jupiter.api.Test;

public class ListenUntilValidationTest {

  @Test
  public void testUntilWithAllThrowsException() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              try (WorkflowApplication app = WorkflowApplication.builder().build()) {
                Workflow workflow =
                    FuncWorkflowBuilder.workflow("test-all-until-invalid")
                        .tasks(
                            listen(
                                "waitOrders",
                                toAll("order.created")
                                    .until(
                                        (io.serverlessworkflow.impl.WorkflowModelCollection
                                                events) -> events.stream().count() >= 3,
                                        io.serverlessworkflow.impl.WorkflowModelCollection.class)))
                        .build();

                app.workflowDefinition(workflow);
              }
            });

    assertTrue(exception.getMessage().contains("until() is only supported with any()"));
    assertTrue(exception.getMessage().contains("ALL"));
  }

  @Test
  public void testUntilWithOneThrowsException() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              try (WorkflowApplication app = WorkflowApplication.builder().build()) {
                Workflow workflow =
                    FuncWorkflowBuilder.workflow("test-one-until-invalid")
                        .tasks(
                            listen(
                                "waitOrders",
                                toOne("order.created")
                                    .until(
                                        (io.serverlessworkflow.impl.WorkflowModelCollection
                                                events) -> events.stream().count() >= 3,
                                        io.serverlessworkflow.impl.WorkflowModelCollection.class)))
                        .build();

                app.workflowDefinition(workflow);
              }
            });

    assertTrue(exception.getMessage().contains("until() is only supported with any()"));
    assertTrue(exception.getMessage().contains("ONE"));
  }
}
