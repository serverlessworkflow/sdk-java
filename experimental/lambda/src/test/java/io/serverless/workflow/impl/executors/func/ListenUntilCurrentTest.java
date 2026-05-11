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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toAny;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.events.EventPublisher;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenUntilCurrentTest {

  private static final Logger log = LoggerFactory.getLogger(ListenUntilCurrentTest.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CloudEvent createOrderEvent(String instanceId, int orderNum) {
    Order order = new Order("order-" + orderNum, "PENDING", 100.0 * orderNum);
    try {
      return CloudEventBuilder.v1()
          .withId("event-" + orderNum)
          .withSource(URI.create("test:/orders"))
          .withType("order.created")
          .withExtension("instanceid", instanceId)
          .withData("application/json", MAPPER.writeValueAsBytes(order))
          .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create order event", e);
    }
  }

  @Test
  public void testCurrentToAnyWithUntilExpression() throws Exception {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow("test-toany-until")
              .tasks(
                  listen(
                      "waitOrders",
                      toAny("order.created")
                          .until(
                              (WorkflowModelCollection events) -> {
                                log.info("Predicate called!");
                                log.info("  Param type: {}", events.getClass().getName());
                                log.info("  Param value: {}", events);
                                log.info("  Event count: {}", (long) events.size());
                                boolean result = (long) events.size() >= 3;
                                log.info("  Returning: {}", result);
                                return result;
                              },
                              WorkflowModelCollection.class)))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      WorkflowInstance instance = definition.instance(new Object());
      CompletableFuture<WorkflowModel> future = instance.start();

      // Wait for WAITING status
      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.WAITING);

      EventPublisher publisher = app.eventPublishers().iterator().next();

      // Emit 3 order events
      log.info("Publishing event 1...");
      publisher.publish(createOrderEvent(instance.id(), 1)).toCompletableFuture().join();

      log.info("Publishing event 2...");
      publisher.publish(createOrderEvent(instance.id(), 2)).toCompletableFuture().join();

      log.info("Publishing event 3...");
      publisher.publish(createOrderEvent(instance.id(), 3)).toCompletableFuture().join();

      // Workflow should complete after 3 events
      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.COMPLETED);

      WorkflowModel result = future.join();
      long count = ((WorkflowModelCollection) result).size();
      log.info("Workflow completed with {} items", count);
      assertEquals(3, count);
    }
  }

  @Test
  public void testToAnyWithUntilContextPredicate() {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow("test-toany-until-context")
              .tasks(
                  listen(
                      "waitOrders",
                      toAny("order.created")
                          .until(
                              (events, context) -> {
                                log.info("ContextPredicate called!");
                                log.info("  Events count: {}", (long) events.size());
                                assertNotNull(context);
                                log.info("  Context instance id: {}", context.instanceData().id());
                                // Stop after 2 events
                                return (long) events.size() >= 2;
                              },
                              WorkflowModelCollection.class)))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      WorkflowInstance instance = definition.instance(new Object());
      CompletableFuture<WorkflowModel> future = instance.start();

      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.WAITING);

      EventPublisher publisher = app.eventPublishers().iterator().next();

      log.info("Publishing event 1...");
      publisher.publish(createOrderEvent(instance.id(), 1)).toCompletableFuture().join();

      log.info("Publishing event 2...");
      publisher.publish(createOrderEvent(instance.id(), 2)).toCompletableFuture().join();

      // Should complete after 2 events
      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.COMPLETED);

      WorkflowModel result = future.join();
      long count = ((WorkflowModelCollection) result).size();
      log.info("Workflow completed with {} items", count);
      assertEquals(2, count);
    }
  }

  @Test
  public void testToAnyWithUntilFilterPredicate() {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      Workflow workflow =
          FuncWorkflowBuilder.workflow("test-toany-until-filter")
              .tasks(
                  listen(
                      "waitOrders",
                      toAny("order.created")
                          .until(
                              (events, workflowCtx, taskCtx) -> {
                                log.info("FilterPredicate called!");
                                log.info("  Events count: {}", (long) events.size());
                                assertNotNull(workflowCtx);
                                assertNotNull(taskCtx);
                                log.info("  Task position: {}", taskCtx.position());
                                return (long) events.size() >= 3;
                              },
                              WorkflowModelCollection.class)))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      WorkflowInstance instance = definition.instance(new Object());
      CompletableFuture<WorkflowModel> future = instance.start();

      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.WAITING);

      EventPublisher publisher = app.eventPublishers().iterator().next();

      log.info("Publishing event 1...");
      publisher.publish(createOrderEvent(instance.id(), 1)).toCompletableFuture().join();

      log.info("Publishing event 2...");
      publisher.publish(createOrderEvent(instance.id(), 2)).toCompletableFuture().join();

      log.info("Publishing event 3...");
      publisher.publish(createOrderEvent(instance.id(), 3)).toCompletableFuture().join();

      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.COMPLETED);

      WorkflowModel result = future.join();
      long count = ((WorkflowModelCollection) result).size();
      log.info("Workflow completed with {} items", count);
      assertEquals(3, count);
    }
  }

  public record Order(String id, String status, double amount) {}
}
