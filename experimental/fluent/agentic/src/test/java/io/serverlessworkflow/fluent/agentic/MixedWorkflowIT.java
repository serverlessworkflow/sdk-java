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

import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.agent;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.doTasks;
import static io.serverlessworkflow.fluent.agentic.dsl.AgenticDSL.function;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class MixedWorkflowIT {
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
                doTasks(
                    function(input -> Map.of("userInput", input), String.class),
                    agent(chatBot),
                    // Here, we are return a simple string so the internal
                    // AgenticScope will add it to the default `input` key
                    // If we want to really manipulate it, we could return a
                    // Map<>(message, input)
                    function(input -> "I've changed the input [" + input + "]", String.class)))
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
}
