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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.tryCatch;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuncTryCatchTest {

  private static final Logger log = LoggerFactory.getLogger(FuncTryCatchTest.class);

  private static final String STOCK_ORDER_ERROR = "ERR_001";
  private static final String PAYMENT_PROCESSING_ERROR = "ERR_002";
  private static final String SHIPPING_ERROR = "ERR_003";

  private static final String ORDER_001 = "ORDER#001";
  private static final String ORDER_002 = "ORDER#002";
  private static final String ORDER_003 = "ORDER#003";

  @Test
  void booking_compensation_dsl() {

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchError(
                                err -> err.type(STOCK_ORDER_ERROR),
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))),
                tryCatch(
                    "tryPaymentProcessing",
                    t ->
                        t.tryCatch(function("paymentProcessing", this::processPayment))
                            .catchWhen(
                                "${ .status == 503 }",
                                function("cancelPayment", this::cancelPayment).then("endFlow"))),
                tryCatch(
                    "tryShipping",
                    t ->
                        t.tryCatch(function("scheduleShipping", this::scheduleShipping))
                            .catchType(
                                SHIPPING_ERROR, function("cancelPayment", this::cancelShipping))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_003).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_003, "endFlow");
    }
  }

  @Test
  void testStockReservationError_CatchByType() {
    log.info("Testing stock reservation error with catch by type");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchError(
                                err -> err.type(STOCK_ORDER_ERROR),
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_001).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_001, "endFlow");
    }
  }

  @Test
  void testStockReservationError_CatchByStatus() {
    log.info("Testing stock reservation error with catch by status code");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchWhen(
                                "${ .status == 409 }",
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_001).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_001, "endFlow");
    }
  }

  @Test
  void testStockReservationError_CatchType() {
    log.info("Testing stock reservation error with catchType");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchType(
                                STOCK_ORDER_ERROR,
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_001).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_001, "endFlow");
    }
  }

  @Test
  void testPaymentProcessingError_CatchByType() {
    log.info("Testing payment processing error with catch by type");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryPaymentProcessing",
                    t ->
                        t.tryCatch(function("paymentProcessing", this::processPayment))
                            .catchError(
                                err -> err.type(PAYMENT_PROCESSING_ERROR),
                                function("cancelPayment", this::cancelPayment).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_002).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_002, "endFlow");
    }
  }

  @Test
  void testPaymentProcessingError_CatchByStatus() {
    log.info("Testing payment processing error with catch by status code");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryPaymentProcessing",
                    t ->
                        t.tryCatch(function("paymentProcessing", this::processPayment))
                            .catchWhen(
                                "${ .status == 503 }",
                                function("cancelPayment", this::cancelPayment).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_002).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_002, "endFlow");
    }
  }

  @Test
  void testPaymentProcessingError_CatchType() {
    log.info("Testing payment processing error with catchType");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryPaymentProcessing",
                    t ->
                        t.tryCatch(function("paymentProcessing", this::processPayment))
                            .catchType(
                                PAYMENT_PROCESSING_ERROR,
                                function("cancelPayment", this::cancelPayment).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_002).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_002, "endFlow");
    }
  }

  @Test
  void testShippingError_CatchByType() {
    log.info("Testing shipping error with catch by type");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryShipping",
                    t ->
                        t.tryCatch(function("scheduleShipping", this::scheduleShipping))
                            .catchError(
                                err -> err.type(SHIPPING_ERROR),
                                function("cancelShipping", this::cancelShipping).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_003).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_003, "endFlow");
    }
  }

  @Test
  void testShippingError_CatchByStatus() {
    log.info("Testing shipping error with catch by status code");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryShipping",
                    t ->
                        t.tryCatch(function("scheduleShipping", this::scheduleShipping))
                            .catchWhen(
                                "${ .status == 500 }",
                                function("cancelShipping", this::cancelShipping).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_003).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_003, "endFlow");
    }
  }

  @Test
  void testShippingError_CatchType() {
    log.info("Testing shipping error with catchType");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryShipping",
                    t ->
                        t.tryCatch(function("scheduleShipping", this::scheduleShipping))
                            .catchType(
                                SHIPPING_ERROR,
                                function("cancelShipping", this::cancelShipping).then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_003).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_003, "endFlow");
    }
  }

  @Test
  void testSuccessfulFlow_NoErrors() {
    log.info("Testing successful flow without any errors");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchError(
                                err -> err.type(STOCK_ORDER_ERROR),
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))),
                tryCatch(
                    "tryPaymentProcessing",
                    t ->
                        t.tryCatch(function("paymentProcessing", this::processPayment))
                            .catchWhen(
                                "${ .status == 503 }",
                                function("cancelPayment", this::cancelPayment).then("endFlow"))),
                tryCatch(
                    "tryShipping",
                    t ->
                        t.tryCatch(function("scheduleShipping", this::scheduleShipping))
                            .catchType(
                                SHIPPING_ERROR, function("cancelShipping", this::cancelShipping))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      // Using a different order ID that doesn't trigger any errors
      WorkflowModel workflowModel = workflowDefinition.instance("ORDER#999").start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains("ORDER#999", "endFlow");
    }
  }

  @Test
  void testMultipleCatchHandlers_FirstMatches() {
    log.info("Testing multiple catch handlers where first one matches");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchError(
                                err -> err.type(STOCK_ORDER_ERROR),
                                function("cancelStockReservation", this::cancelReservation)
                                    .then("endFlow"))
                            .catchWhen(
                                "${ .status == 409 }",
                                function("alternativeCancellation", this::cancelReservation)
                                    .then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_001).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_001, "endFlow");
    }
  }

  @Test
  void testCatchAll_WithAnyError() {
    log.info("Testing catch-all handler for any error");

    Workflow workflow =
        FuncWorkflowBuilder.workflow()
            .tasks(
                tryCatch(
                    "tryStockReservation",
                    t ->
                        t.tryCatch(function("stockReservation", this::reserveStock))
                            .catchWhen(
                                "${ true }",
                                function("genericErrorHandler", this::cancelReservation)
                                    .then("endFlow"))),
                function("endFlow", this::endFlow))
            .build();

    try (WorkflowApplication application = WorkflowApplication.builder().build()) {
      WorkflowDefinition workflowDefinition = application.workflowDefinition(workflow);
      WorkflowModel workflowModel = workflowDefinition.instance(ORDER_001).start().join();

      assertThat(workflowModel.asCollection())
          .map(w -> w.asText().orElseThrow())
          .contains(ORDER_001, "endFlow");
    }
  }

  public String reserveStock(String order) {
    log.info("Reserving stock for order: {}", order);
    if (order.equals(ORDER_001)) {
      throw new WorkflowException(WorkflowError.error(STOCK_ORDER_ERROR, 409).build());
    }
    return order;
  }

  public String cancelReservation(String order) {
    log.info("Cancelling reservation for order: {}", order);
    return order;
  }

  public String processPayment(String order) {
    log.info("Processing payment for order: {}", order);
    if (order.equals(ORDER_002)) {
      throw new WorkflowException(WorkflowError.error(PAYMENT_PROCESSING_ERROR, 503).build());
    }
    return order;
  }

  public String cancelPayment(String order) {
    log.info("Cancel payment for order: {}", order);
    cancelReservation(order);
    return order;
  }

  public String scheduleShipping(String order) {
    log.info("Scheduling shipping for order: {}", order);
    if (order.equals(ORDER_003)) {
      throw new WorkflowException(WorkflowError.error(SHIPPING_ERROR, 500).build());
    }
    return order;
  }

  public String cancelShipping(String order) {
    log.info("Cancel shipping for order: {}", order);
    cancelReservation(order);
    return order;
  }

  public List<String> endFlow(String order) {
    log.info("End flow for order:  {}", order);
    return List.of(order, "endFlow");
  }
}
