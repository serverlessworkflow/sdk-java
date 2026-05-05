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
import static org.awaitility.Awaitility.await;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.lifecycle.TraceExecutionListener;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

public class ForEachFuncTest {

  private record Order(String id) {}

  private record EnhancedOrder(String id, int salary) {}

  private record OrdersPayload(List<Order> orders) {}

  private record OrderName(String id, String name) {}

  @Test
  void testForEachIteration() {

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

  @Test
  void testForEachEmit() {

    String eventType = "test.item.emitted";
    Workflow workflow =
        FuncWorkflowBuilder.workflow("forEach-bug-reproducer")
            .tasks(
                // ForEach should emit 3 events, one per item
                forEach(
                    (Collection<Map<String, String>> items) -> items,
                    emitJson(eventType, Map.class).inputFrom("$item")))
            .build();

    List<CloudEvent> publishedEvents = new CopyOnWriteArrayList<>();
    LaggedInMemoryEvents eventBroker = new LaggedInMemoryEvents();
    eventBroker.register(eventType, ce -> publishedEvents.add(ce));

    try (WorkflowApplication app =
        WorkflowApplication.builder()
            .withEventConsumer(eventBroker)
            .withEventPublisher(eventBroker)
            .withListener(new TraceExecutionListener())
            .build()) {
      app.workflowDefinition(workflow)
          .instance(
              List.of(
                  new OrderName("item-1", "first"),
                  new OrderName("item-2", "second"),
                  new OrderName("item-3", "third")))
          .start()
          .join();
      await()
          .atMost(Duration.ofSeconds(2))
          .pollInterval(Duration.ofMillis(10))
          .until(() -> publishedEvents.size() == 3);

      assertThat(
              publishedEvents.stream()
                  .map(CloudEvent::getData)
                  .map(PojoCloudEventData.class::cast)
                  .map(p -> p.getValue())
                  .toList())
          .isEqualTo(
              List.of(
                  Map.of("id", "item-1", "name", "first"),
                  Map.of("id", "item-2", "name", "second"),
                  Map.of("id", "item-3", "name", "third")));
    }
  }

  private static EnhancedOrder enhace(Order order) {
    return new EnhancedOrder(order.id(), 1000);
  }
}
