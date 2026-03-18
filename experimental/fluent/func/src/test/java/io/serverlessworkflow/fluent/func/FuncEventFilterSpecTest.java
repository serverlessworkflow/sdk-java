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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.Workflow;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Event Filter DSL specification. Verifies that the fluent builder correctly wires
 * the payload parsing and contextual lambdas into the final Workflow definitions.
 */
class FuncEventFilterSpecTest {

  // Dummy POJO for dataAs(Class<T>) testing
  static class TestOrder {
    private int id;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }
  }

  @Test
  @DisplayName("consumed(...).dataAsMap builds successfully into a ListenTask")
  void dataAsMap_compilesAndBuildsListenTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-data-as-map")
            .tasks(
                listen(
                    "listenMap",
                    toOne(consumed("org.test.event").dataAsMap(map -> map.containsKey("orderId")))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected for dataAsMap filter");
  }

  @Test
  @DisplayName("consumed(...).dataAs(Class) builds successfully into a ListenTask")
  void dataAsClass_compilesAndBuildsListenTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-data-as-class")
            .tasks(
                listen(
                    "listenClass",
                    toOne(
                        consumed("org.test.event")
                            .dataAs(TestOrder.class, order -> order.getId() > 0))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected for dataAs(Class) filter");
  }

  @Test
  @DisplayName("consumed(...).dataFields builds successfully into a ListenTask")
  void dataFields_compilesAndBuildsListenTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-data-fields")
            .tasks(
                listen(
                    "listenFields",
                    toOne(consumed("org.test.event").dataFields("orderId", "customerId"))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected for dataFields filter");
  }

  @Test
  @DisplayName("consumed(...).dataByInstanceId builds successfully into a ListenTask")
  void dataByInstanceId_compilesAndBuildsListenTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-data-instance-id")
            .tasks(
                listen(
                    "listenDataId",
                    toOne(consumed("org.test.event").dataByInstanceId("workflowInstanceId"))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected for dataByInstanceId filter");
  }

  @Test
  @DisplayName("consumed(...).extensionByInstanceId builds successfully into a ListenTask")
  void extensionByInstanceId_compilesAndBuildsListenTask() {
    Workflow wf =
        FuncWorkflowBuilder.workflow("filter-ext-instance-id")
            .tasks(
                listen(
                    "listenExtId",
                    toOne(consumed("org.test.event").extensionByInstanceId("workflowinstanceid"))))
            .build();

    List<TaskItem> items = wf.getDo();
    assertEquals(1, items.size());

    Task t = items.get(0).getTask();
    assertNotNull(t.getListenTask(), "ListenTask expected for extensionByInstanceId filter");
  }
}
