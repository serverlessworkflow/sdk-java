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
package io.serverlessworkflow.fluent.agentic;

import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.cases;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.event;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.fn;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.toAny;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import dev.langchain4j.agentic.AgenticServices;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.acme.EmailDraft;
import org.acme.EmailDrafts;
import org.acme.EmailPolicies;
import org.acme.PolicyDecision;
import org.junit.jupiter.api.Test;

public class EmailDrafterIT {

  @Test
  @SuppressWarnings("unchecked")
  void email_drafter_agent() {
    Agents.EmailDrafter emailDrafter =
        AgenticServices.agentBuilder(Agents.EmailDrafter.class)
            .chatModel(Models.BASE_MODEL)
            .outputKey("email_draft")
            .build();

    BlockingQueue<CloudEvent> finishedEvents = new LinkedBlockingQueue<>();

    final Workflow emailDrafterWorkflow =
        AgentWorkflowBuilder.workflow("emailDrafterAgentic")
            .tasks(
                tasks ->
                    tasks
                        .agent("agentEmailDrafter", emailDrafter)
                        .callFn("parseDraft", fn(EmailDrafts::parse, String.class))
                        .callFn("policyCheck", fn(EmailPolicies::policyCheck, EmailDraft.class))
                        .switchCase(
                            "needsHumanReview?",
                            cases(
                                AgenticDSL.caseOf(
                                        d -> !EmailPolicies.Decision.AUTO_SEND.equals(d.decision()),
                                        PolicyDecision.class)
                                    .then("requestReview"),
                                AgenticDSL.caseDefault("emailFinished")))
                        .emit(
                            "requestReview",
                            event(
                                "org.acme.email.review.required",
                                payload ->
                                    PojoCloudEventData.wrap(
                                        payload,
                                        p ->
                                            JsonUtils.mapper()
                                                .writeValueAsString(payload)
                                                .getBytes()),
                                PolicyDecision.class))
                        .listen(
                            "waitForReview",
                            toAny("org.acme.email.approved", "org.acme.email.denied"))
                        .emit("emailFinished", event("org.acme.email.finished", null)))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      // input
      Map<String, Object> emailVars =
          Map.ofEntries(
              Map.entry("recipientName", "John Mars"),
              Map.entry("senderName", "Rick Venus"),
              Map.entry("purpose", "follow-up"),
              Map.entry(
                  "keyPoints",
                  List.of(
                      "Thanks for the call yesterday",
                      "Attaching the one-page overview",
                      "Available Wed or Thu 2–4pm ET",
                      "Check the links for more")),
              Map.entry("tone", "friendly"), // friendly | neutral | formal
              Map.entry("length", "short"), // short | medium
              Map.entry("cta", "Please reply with a 15-minute slot this week."),
              Map.entry("signature", "Best regards,\nRick Venus\nEngineer\nAcme"),
              Map.entry("allowedDomains", List.of("acme.com", "example.com")),
              Map.entry(
                  "links",
                  List.of(
                      "https://acme.com/proposals/alpha", "https://example.com/schedule/rick")));

      // Listen to de event
      app.eventConsumer()
          .register(
              app.eventConsumer()
                  .listen(
                      new EventFilter()
                          .withWith(new EventProperties().withType("org.acme.email.finished")),
                      app),
              ce -> finishedEvents.add((CloudEvent) ce));

      var instance = app.workflowDefinition(emailDrafterWorkflow).instance(emailVars);
      var running = instance.start().join();
      var policyDecision = running.as(PolicyDecision.class);
      assertThat(policyDecision).isNotNull();
      assertThat(policyDecision.isPresent()).isTrue();
      assertThat(policyDecision.get().decision()).isEqualTo(EmailPolicies.Decision.AUTO_SEND);
      assertThat(instance.status()).isEqualTo(WorkflowStatus.COMPLETED);

      CloudEvent finished = finishedEvents.poll(1, TimeUnit.SECONDS);
      assertThat(finished).isNotNull();
    } catch (InterruptedException e) {
      fail(e);
    }
  }
}
