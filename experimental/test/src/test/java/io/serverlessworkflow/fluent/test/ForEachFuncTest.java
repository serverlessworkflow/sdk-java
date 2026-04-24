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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ForEachFuncTest {

  private static record Order(String id) {}

  private static record EnhancedOrder(String id, int salary) {}

  private static record OrdersPayload(List<Order> orders) {}

  @Test
  void testForEachIteration() throws Exception {

    Workflow workflow =
        FuncWorkflowBuilder.workflow("foreach-workflow")
            .tasks(forEachItem(OrdersPayload::orders, ForEachFuncTest::enhace))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      OrdersPayload input =
          new OrdersPayload(
              List.of(new Order("ORD-001"), new Order("ORD-002"), new Order("ORD-003")));
      WorkflowModel result = app.workflowDefinition(workflow).instance(input).start().join();
      assertThat(result.as(EnhancedOrder.class).orElseThrow().id())
          .isEqualTo(input.orders().get(input.orders.size() - 1).id());
    }
  }

  private static EnhancedOrder enhace(Order order) {
    return new EnhancedOrder(order.id(), 1000);
  }
}
