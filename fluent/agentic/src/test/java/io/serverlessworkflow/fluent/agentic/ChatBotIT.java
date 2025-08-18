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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.api.types.EventProperties;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ChatBotIT {

  @Test
  @SuppressWarnings("unchecked")
  @Disabled("Figuring out event processing")
  void chat_bot() {
    Agents.ChatBot chatBot =
        spy(
            AgenticServices.agentBuilder(Agents.ChatBot.class)
                .chatModel(Models.BASE_MODEL)
                // .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputName("conversation")
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
                                l.until(
                                        message ->
                                            !message
                                                .getOrDefault("userInput", "")
                                                .toString()
                                                .isEmpty(),
                                        Map.class)
                                    .any(
                                        c ->
                                            c.with(event -> event.type("org.acme.chatbot.request")))
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
                                                                            "org.acme.chatbot.reply"))))))
                        .emit(
                            emit ->
                                emit.when(
                                        message ->
                                            message
                                                .getOrDefault("userInput", "")
                                                .toString()
                                                .isEmpty(),
                                        Map.class)
                                    .event(e -> e.type("org.acme.chatbot.finished"))))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      app.eventConsumer()
          .register(
              app.eventConsumer()
                  .listen(
                      new EventFilter()
                          .withWith(new EventProperties().withType("org.acme.chatbot.reply")),
                      app),
              ce -> replyEvents.add((CloudEvent) ce));

      app.eventConsumer()
          .register(
              app.eventConsumer()
                  .listen(
                      new EventFilter()
                          .withWith(new EventProperties().withType("org.acme.chatbot.finished")),
                      app),
              ce -> finishedEvents.add((CloudEvent) ce));

      final WorkflowInstance waitingInstance =
          app.workflowDefinition(listenWorkflow).instance(Map.of());
      final CompletableFuture<WorkflowModel> runningModel = waitingInstance.start();

      // The workflow is just waiting for the event
      assertEquals(WorkflowStatus.WAITING, waitingInstance.status());

      // Publish the event
      app.eventPublisher().publish(newMessageEvent("Hello World!"));
      CloudEvent reply = replyEvents.poll(60, TimeUnit.SECONDS);
      assertNotNull(reply);

      // Empty message completes the workflow
      app.eventPublisher().publish(newMessageEvent(""));
      CloudEvent finished = finishedEvents.poll(60, TimeUnit.SECONDS);
      assertNotNull(finished);

      assertThat(runningModel).isCompleted();
      assertEquals(WorkflowStatus.COMPLETED, waitingInstance.status());

    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  /**
   * In this test we validate a workflow mixed with agents and regular Java calls
   *
   * <p>
   *
   * <ol>
   *   <li>The first function prints the message input and converts the data into a Map for the
   *       agent ingestion
   *   <li>Internally, our factories will add the output to a new AgenticScope since under the hood,
   *       we are call `as(AgenticScope)`
   *   <li>The agent is then called with a scope with a state as `message="input"`
   *   <li>The agent updates the state automatically in the AgenticScope and returns the message as
   *       a string, this string is then served to the next task
   *   <li>The next task process the agent response and returns it ending the workflow. Meanwhile,
   *       the AgenticScope is always updated with the latest result from the given task.
   * </ol>
   */
  @Test
  void mixed_workflow() {
    Agents.ChatBot chatBot =
        spy(
            AgenticServices.agentBuilder(Agents.ChatBot.class)
                .chatModel(Models.BASE_MODEL)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputName("userInput")
                .build());

    final Workflow mixedWorkflow =
        AgentWorkflowBuilder.workflow("chat-bot")
            .tasks(
                t ->
                    t.callFn(
                            callJ ->
                                callJ.function(
                                    input -> {
                                      System.out.println(input);
                                      return Map.of("userInput", input);
                                    },
                                    String.class))
                        .agent(chatBot)
                        .callFn(
                            callJ ->
                                callJ.function(
                                    input -> {
                                      System.out.println(input);
                                      // Here, we are return a simple string so the internal
                                      // AgenticScope will add it to the default `input` key
                                      // If we want to really manipulate it, we could return a
                                      // Map<>(message, input)
                                      return "I've changed the input [" + input + "]";
                                    },
                                    String.class)))
            .build();

    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      WorkflowModel model =
          app.workflowDefinition(mixedWorkflow).instance("Hello World!").start().join();

      Optional<String> resultAsString = model.as(String.class);

      assertTrue(resultAsString.isPresent());
      assertFalse(resultAsString.get().isEmpty());
      assertTrue(resultAsString.get().contains("changed the input"));

      Optional<AgenticScope> resultAsScope = model.as(AgenticScope.class);

      assertTrue(resultAsScope.isPresent());
      assertFalse(resultAsScope.get().readState("input").toString().isEmpty());
      assertTrue(resultAsScope.get().readState("input").toString().contains("changed the input"));
    }
  }

  private CloudEvent newMessageEvent(String message) {
    return new CloudEventBuilder()
        .withData(String.format("{\"userInput\": \"%s\"}", message).getBytes())
        .withType("org.acme.chatbot.request")
        .withId(UUID.randomUUID().toString())
        .withDataContentType("application/json")
        .withSource(URI.create("test://localhost"))
        .withSubject("A chatbot message")
        .withTime(OffsetDateTime.now())
        .build();
  }
}
