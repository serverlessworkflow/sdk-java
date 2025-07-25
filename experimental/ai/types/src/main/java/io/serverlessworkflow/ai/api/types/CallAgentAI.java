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

package io.serverlessworkflow.ai.api.types;

import dev.langchain4j.agentic.AgentServices;
import dev.langchain4j.agentic.internal.AgentInstance;
import dev.langchain4j.model.chat.ChatModel;
import io.serverlessworkflow.api.types.TaskBase;
import java.util.Objects;

public class CallAgentAI extends TaskBase {

  private final AgentInstance instance;

  public CallAgentAI(Object instance) {
    if (!(instance instanceof AgentInstance)) {
      throw new IllegalArgumentException("Instance must be subtype of AgentInstance");
    }

    this.instance = (AgentInstance) instance;
  }

  public AgentInstance getAgentInstance() {
    return instance;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Class<?> agentClass;

    private ChatModel chatModel;

    private String outputName;

    private Builder() {}

    public CallAgentAI.Builder withAgentClass(Class<?> agentClass) {
      this.agentClass = agentClass;
      return this;
    }

    public CallAgentAI.Builder withChatModel(ChatModel chatModel) {
      this.chatModel = chatModel;
      return this;
    }

    public CallAgentAI.Builder withOutputName(String outputName) {
      this.outputName = outputName;
      return this;
    }

    public CallAgentAI build() {
      Objects.requireNonNull(agentClass, "agentClass must be provided");
      Objects.requireNonNull(chatModel, "chatModel must be provided");
      Objects.requireNonNull(outputName, "outputName must be provided");

      if (outputName.isBlank()) {
        throw new IllegalArgumentException("outputName must not be blank");
      }

      return new CallAgentAI(
          AgentServices.agentBuilder(agentClass)
              .chatModel(chatModel)
              .outputName(outputName)
              .build());
    }
  }
}
