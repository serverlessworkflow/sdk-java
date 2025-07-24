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

package io.serverlessworkflow.impl.executors.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import io.serverlessworkflow.api.types.ai.CallAIChatModel;
import io.serverlessworkflow.impl.services.ChatModelService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallAIChatModelExecutor implements AIChatModelExecutor {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)\\s*\\}\\}");

  private final CallAIChatModel callAIChatModel;

  public CallAIChatModelExecutor(CallAIChatModel callAIChatModel) {
    this.callAIChatModel = callAIChatModel;
  }

  @Override
  public Object apply(Object javaObject) {
    validate(callAIChatModel, javaObject);

    ChatModel chatModel = createChatModel(callAIChatModel);
    Map<String, Object> substitutions = (Map<String, Object>) javaObject;

    List<ChatMessage> messages = new ArrayList<>();

    if (callAIChatModel.getChatModelRequest().getSystemMessages() != null) {
      for (String systemMessage : callAIChatModel.getChatModelRequest().getSystemMessages()) {
        String fixedUserMessage = replaceVariables(systemMessage, substitutions);
        messages.add(new SystemMessage(fixedUserMessage));
      }
    }

    if (callAIChatModel.getChatModelRequest().getUserMessages() != null) {
      for (String userMessage : callAIChatModel.getChatModelRequest().getUserMessages()) {
        String fixedUserMessage = replaceVariables(userMessage, substitutions);
        messages.add(new UserMessage(fixedUserMessage));
      }
    }

    return prepareResponse(chatModel.chat(messages), javaObject);
  }

  private ChatModel createChatModel(CallAIChatModel callAIChatModel) {
    ChatModelService chatModelService = getAvailableModel();
    if (chatModelService != null) {
      return chatModelService.getChatModel(callAIChatModel.getPreferences());
    }
    throw new IllegalStateException(
        "No LLM models found. Please ensure that you have the required dependencies in your classpath.");
  }

  private String replaceVariables(String template, Map<String, Object> substitutions) {
    Set<String> variables = extractVariables(template);
    for (Map.Entry<String, Object> entry : substitutions.entrySet()) {
      String variable = entry.getKey();
      Object value = entry.getValue();
      if (value != null && variables.contains(variable)) {
        template = template.replace("{{" + variable + "}}", value.toString());
      }
    }
    return template;
  }

  private ChatModelService getAvailableModel() {
    ServiceLoader<ChatModelService> loader = ServiceLoader.load(ChatModelService.class);

    for (ChatModelService service : loader) {
      return service;
    }

    throw new IllegalStateException(
        "No LLM models found. Please ensure that you have the required dependencies in your classpath.");
  }

  private static Set<String> extractVariables(String template) {
    Set<String> variables = new HashSet<>();
    Matcher matcher = VARIABLE_PATTERN.matcher(template);
    while (matcher.find()) {
      variables.add(matcher.group(1));
    }
    return variables;
  }

  private void validate(CallAIChatModel callAIChatModel, Object javaObject) {
    // TODO
  }

  protected Map<String, Object> prepareResponse(ChatResponse response, Object javaObject) {
    String id = response.id();
    String modelName = response.modelName();
    TokenUsage tokenUsage = response.tokenUsage();
    FinishReason finishReason = response.finishReason();
    AiMessage aiMessage = response.aiMessage();

    Map<String, Object> responseMap = (Map<String, Object>) javaObject;
    if (response.id() != null) {
      responseMap.put("id", id);
    }

    if (modelName != null) {
      responseMap.put("modelName", modelName);
    }

    if (tokenUsage != null) {
      responseMap.put("tokenUsage.inputTokenCount", tokenUsage.inputTokenCount());
      responseMap.put("tokenUsage.outputTokenCount", tokenUsage.outputTokenCount());
      responseMap.put("tokenUsage.totalTokenCount", tokenUsage.totalTokenCount());
    }

    if (finishReason != null) {
      responseMap.put("finishReason", finishReason.name());
    }

    if (aiMessage != null) {
      responseMap.put("result", aiMessage.text());
    }

    return responseMap;
  }
}
