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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonCloudEventData;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.events.InMemoryEvents;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class ChatBotIT {

  @Test
  @SuppressWarnings("unchecked")
  void chat_bot() {
    final ObjectMapper mapper = new ObjectMapper();
    Agents.ChatBot chatBot =
        spy(
            AgenticServices.agentBuilder(Agents.ChatBot.class)
                .chatModel(Models.BASE_MODEL)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("conversation")
                .build());
    BlockingQueue<CloudEvent> replyEvents = new LinkedBlockingQueue<>();
    BlockingQueue<CloudEvent> finishedEvents = new LinkedBlockingQueue<>();

    // 1. listen to an event containing `message` key in the body
    // 2. if contains, call the agent, if not end the workflow
    // 3. After replying to the chat, return
    final Workflow listenWorkflow =
        AgentWorkflowBuilder.workflow("chat-bot")
            .tasks(
                t ->
                    t.listen(
                            l ->
                                l.to(
                                        to ->
                                            to.any(
                                                    c ->
                                                        c.with(
                                                            event ->
                                                                event.type(
                                                                    "org.acme.chatbot.request")))
                                                .until(
                                                    until ->
                                                        until.one(
                                                            one ->
                                                                one.with(
                                                                    e ->
                                                                        e.type(
                                                                            "org.acme.chatbot.finalize")))))
                                    .forEach(
                                        f ->
                                            f.tasks(
                                                tasks ->
                                                    tasks
                                                        .agent(chatBot)
                                                        .emit(
                                                            emit ->
                                                                emit.event(
                                                                    e ->
                                                                        e.type(
                                                                                "org.acme.chatbot.reply")
                                                                            .data(
                                                                                convo -> {
                                                                                  var node =
                                                                                      JsonUtils
                                                                                          .object()
                                                                                          .put(
                                                                                              "conversation",
                                                                                              convo
                                                                                                  .getOrDefault(
                                                                                                      "conversation",
                                                                                                      "")
                                                                                                  .toString());
                                                                                  return JsonCloudEventData
                                                                                      .wrap(node);
                                                                                },
                                                                                Map.class))))))
                        .emit(emit -> emit.event(e -> e.type("org.acme.chatbot.finished"))))
            .build();

    InMemoryEvents eventBroker = new InMemoryEvents();
    eventBroker.register("org.acme.chatbot.reply", ce -> replyEvents.add((CloudEvent) ce));
    eventBroker.register("org.acme.chatbot.finished", ce -> finishedEvents.add((CloudEvent) ce));

    try (WorkflowApplication app =
        WorkflowApplication.builder()
            .withEventConsumer(eventBroker)
            .withEventPublisher(eventBroker)
            .build()) {
      final WorkflowInstance waitingInstance =
          app.workflowDefinition(listenWorkflow).instance(Map.of());
      final CompletableFuture<WorkflowModel> runningModel = waitingInstance.start();

      // The workflow is just waiting for the event
      assertEquals(WorkflowStatus.WAITING, waitingInstance.status());

      // Publish the events

      eventBroker.publish(newRequestMessage("Hi! Can you tell me a good duck joke?"));

      CloudEvent reply = replyEvents.poll(60, TimeUnit.SECONDS);
      assertNotNull(reply);

      eventBroker.publish(newRequestMessage("Oh I didn't like this one, please tell me another."));
      reply = replyEvents.poll(60, TimeUnit.SECONDS);
      assertNotNull(reply);
      assertThat(
              ((JsonCloudEventData) Objects.requireNonNull(reply.getData()))
                  .getNode()
                  .get("conversation")
                  .asText())
          .contains("No worries");

      // Empty message completes the workflow
      eventBroker.publish(newFinalizeMessage());
      CloudEvent finished = finishedEvents.poll(60, TimeUnit.SECONDS);
      assertNotNull(finished);
      assertThat(finishedEvents).isEmpty();

      assertThat(runningModel).isCompleted();
      assertEquals(WorkflowStatus.COMPLETED, waitingInstance.status());

    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  private CloudEvent newRequestMessage(String message) {
    return CloudEventsTestBuilder.newMessage(
        String.format("{\"userInput\": \"%s\"}", message), "org.acme.chatbot.request");
  }

  private CloudEvent newFinalizeMessage() {
    return CloudEventsTestBuilder.newMessage("", "org.acme.chatbot.finalize");
  }
}
