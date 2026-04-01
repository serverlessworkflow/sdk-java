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

import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consume;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.consumed;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.emitJson;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.function;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.listen;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.switchWhenOrElse;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.to;
import static io.serverlessworkflow.fluent.func.dsl.FuncDSL.toOne;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.func.FuncWorkflowBuilder;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.events.EventPublisher;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Event Filter DSL specification. Verifies that the fluent builder correctly wires
 * the payload parsing and contextual lambdas into the final Workflow definitions, and ensures the
 * ModelCollection adapters seamlessly convert between types.
 */
class FuncEventFilterTest {

  @Test
  void testListenToOneCollection() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewCol")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((Collection<?> node) -> node.iterator().next()))
            .build());
  }

  @Test
  void testListenToOneArrayNode() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewNode")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((ArrayNode node) -> node.get(0)))
            .build());
  }

  @Test
  void testListenToOneList() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewList")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((List<JsonNode> list) -> list.get(0)))
            .build());
  }

  @Test
  void testListenToOneSet() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewSet")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((Set<JsonNode> set) -> set.iterator().next()))
            .build());
  }

  @Test
  void testListenToOneArray() {
    runIt(
        FuncWorkflowBuilder.workflow("listenToOneReviewArray")
            .tasks(
                listen("waitReview", toOne("org.acme.test.review"))
                    .outputAs((JsonNode[] array) -> array[0]))
            .build());
  }

  private Workflow reviewEmitter() {
    return FuncWorkflowBuilder.workflow("emitReview")
        .tasks(emitJson("draftReady", "org.acme.test.review", Review.class))
        .build();
  }

  private void runIt(Workflow listen) {
    Review review = new Review("Torrente", "espectacular", 5);

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      CompletableFuture<WorkflowModel> waiting =
          app.workflowDefinition(listen).instance(Map.of()).start();

      app.workflowDefinition(reviewEmitter()).instance(review).start().join();

      assertThat(waiting.join().as(Review.class).orElseThrow()).isEqualTo(review);
    }
  }

  // --- Mock Service Methods ---
  NewsletterDraft writeDraft(NewsletterRequest req) {
    return new NewsletterDraft("Draft: " + req.topic(), "Initial body...");
  }

  NewsletterDraft editDraft(HumanReview review) {
    return new NewsletterDraft("Edited Draft", "Fixed based on: " + review.notes());
  }

  void sendEmail(NewsletterDraft draft) {
    // Simulates MailService.send
  }

  @Test
  void testJacksonAutomagicalConversion() throws Exception {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {

      Workflow workflow =
          FuncWorkflowBuilder.workflow("intelligent-newsletter")
              .tasks(
                  function("draftAgent", this::writeDraft).exportAsTaskOutput(),
                  emitJson("draftReady", "org.acme.email.review.required", NewsletterDraft.class),
                  listen(
                          "waitHumanReview",
                          to().one(
                                  consumed("org.acme.newsletter.review.done")
                                      .extensionByInstanceId("instanceid")))
                      .outputAs((Collection<?> events) -> events.iterator().next()),
                  // The engine sees the incoming JsonNode, sees this task expects
                  // HumanReview.class,
                  // and natively deserializes it for you before executing the lambda!
                  switchWhenOrElse(
                      h -> HumanReview.NEEDS_REVISION.equals(h.status()),
                      "humanEditorAgent",
                      "sendNewsletter",
                      HumanReview.class),
                  function("humanEditorAgent", this::editDraft)
                      .exportAsTaskOutput()
                      .then("draftReady"),
                  consume("sendNewsletter", this::sendEmail)
                      // Because we are in Jackson, the payload at this evaluation stage can be a
                      // Map.
                      // We simply check for the "status" field to know if it's the review payload.
                      .inputFrom(
                          (Map<String, Object> payload,
                              WorkflowContextData wfc,
                              TaskContextData tfc) ->
                              payload.containsKey("status") ? wfc.context() : payload))
              .build();

      WorkflowDefinition definition = app.workflowDefinition(workflow);
      WorkflowInstance instance = definition.instance(new NewsletterRequest("Tech Stocks"));
      CompletableFuture<WorkflowModel> future = instance.start();

      await()
          .atMost(Duration.ofSeconds(5))
          .until(() -> instance.status() == WorkflowStatus.WAITING);

      CloudEvent humanReviewEvent =
          CloudEventBuilder.v1()
              .withId("event-123")
              .withSource(URI.create("test:/human-editor"))
              .withType("org.acme.newsletter.review.done")
              .withExtension("instanceid", instance.id())
              .withData(
                  "application/json",
                  "{\"status\":\"APPROVED\", \"notes\":\"Looks good\"}"
                      .getBytes(StandardCharsets.UTF_8))
              .build();

      EventPublisher publisher = app.eventPublishers().iterator().next();
      publisher.publish(humanReviewEvent).toCompletableFuture().join();

      future.join();

      assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);
    }
  }

  record Review(String author, String text, int rating) {}

  record NewsletterRequest(String topic) {}

  record NewsletterDraft(String title, String body) {}

  record HumanReview(String status, String notes) {
    public static final String NEEDS_REVISION = "NEEDS_REVISION";
    public static final String APPROVED = "APPROVED";
  }
}
