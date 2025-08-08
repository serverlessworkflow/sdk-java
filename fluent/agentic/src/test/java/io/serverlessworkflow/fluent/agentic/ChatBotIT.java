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

import static org.mockito.Mockito.spy;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.junit.jupiter.api.Test;

public class ChatBotIT {

  @Test
  void chat_bot() {
    Agents.ChatBot chatBot =
        spy(
            AgenticServices.agentBuilder(Agents.ChatBot.class)
                .chatModel(Models.BASE_MODEL)
                .outputName("message")
                .build());
    // 1. listen to an event containing `message` key in the body
    // 2. if contains, call the agent, if not end the workflow
    // 3. After replying to the chat, return
    AgentWorkflowBuilder.workflow("chat-bot")
        .tasks(
            t ->
                t.listen(
                        "listenToMessages",
                        l ->
                            l.outputAs(null)
                                .one(c -> c.with(event -> event.type("org.acme.chatbot"))))
                    .when(scope -> !"".equals(scope.readState("message")), AgenticScope.class)
                    .agent(chatBot)
                    .then("listenToMessages"));
  }
}
