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

package io.serverlessworkflow.impl.services.openai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.serverlessworkflow.api.types.ai.CallAIChatModel;
import io.serverlessworkflow.impl.services.ChatModelService;

public class OpenAIChatModelService implements ChatModelService {

  @Override
  public ChatModel getChatModel(CallAIChatModel.ChatModelPreferences chatModelPreferences) {
    OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder();
    if (chatModelPreferences.getApiKey() != null) {
      builder.apiKey(chatModelPreferences.getApiKey());
    }
    if (chatModelPreferences.getModelName() != null) {
      builder.modelName(chatModelPreferences.getModelName());
    }
    if (chatModelPreferences.getBaseUrl() != null) {
      builder.baseUrl(chatModelPreferences.getBaseUrl());
    }
    if (chatModelPreferences.getMaxTokens() != null) {
      builder.maxTokens(chatModelPreferences.getMaxTokens());
    }
    if (chatModelPreferences.getTemperature() != null) {
      builder.temperature(chatModelPreferences.getTemperature());
    }
    if (chatModelPreferences.getTopP() != null) {
      builder.topP(chatModelPreferences.getTopP());
    }
    if (chatModelPreferences.getResponseFormat() != null) {
      builder.responseFormat(chatModelPreferences.getResponseFormat());
    }
    if (chatModelPreferences.getMaxRetries() != null) {
      builder.maxRetries(chatModelPreferences.getMaxRetries());
    }
    if (chatModelPreferences.getTimeout() != null) {
      builder.timeout(chatModelPreferences.getTimeout());
    }
    if (chatModelPreferences.getLogRequests() != null) {
      builder.logRequests(chatModelPreferences.getLogRequests());
    }
    if (chatModelPreferences.getLogResponses() != null) {
      builder.logResponses(chatModelPreferences.getLogResponses());
    }
    if (chatModelPreferences.getResponseFormat() != null) {
      builder.responseFormat(chatModelPreferences.getResponseFormat());
    }

    if (chatModelPreferences.getMaxCompletionTokens() != null) {
      builder.maxCompletionTokens(chatModelPreferences.getMaxCompletionTokens());
    }

    if (chatModelPreferences.getPresencePenalty() != null) {
      builder.presencePenalty(chatModelPreferences.getPresencePenalty());
    }

    if (chatModelPreferences.getFrequencyPenalty() != null) {
      builder.frequencyPenalty(chatModelPreferences.getFrequencyPenalty());
    }

    if (chatModelPreferences.getStrictJsonSchema() != null) {
      builder.strictJsonSchema(chatModelPreferences.getStrictJsonSchema());
    }

    if (chatModelPreferences.getSeed() != null) {
      builder.seed(chatModelPreferences.getSeed());
    }

    if (chatModelPreferences.getUser() != null) {
      builder.user(chatModelPreferences.getUser());
    }

    if (chatModelPreferences.getProjectId() != null) {
      builder.projectId(chatModelPreferences.getProjectId());
    }

    return builder.build();
  }
}
