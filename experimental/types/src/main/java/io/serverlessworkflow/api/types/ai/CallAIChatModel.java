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

package io.serverlessworkflow.api.types.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CallAIChatModel extends AbstractCallAIChatModelTask {

  private ChatModelPreferences chatModelPreferences;

  private ChatModelRequest chatModelRequest;

  protected CallAIChatModel() {}

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return "CallAIChatModel{"
        + "chatModelPreferences="
        + chatModelPreferences
        + ", chatModelRequest="
        + chatModelRequest
        + '}';
  }

  public ChatModelPreferences getPreferences() {
    return chatModelPreferences;
  }

  public ChatModelRequest getChatModelRequest() {
    return chatModelRequest;
  }

  public static class Builder {

    private Builder() {}

    private ChatModelPreferences chatModelPreferences;
    private ChatModelRequest chatModelRequest;

    public Builder preferences(ChatModelPreferences chatModelPreferences) {
      this.chatModelPreferences = chatModelPreferences;
      return this;
    }

    public Builder request(ChatModelRequest chatModelRequest) {
      this.chatModelRequest = chatModelRequest;
      return this;
    }

    public CallAIChatModel build() {
      CallAIChatModel callAIChatModel = new CallAIChatModel();
      callAIChatModel.chatModelPreferences = this.chatModelPreferences;
      callAIChatModel.chatModelRequest = this.chatModelRequest;
      return callAIChatModel;
    }
  }

  public static class ChatModelPreferences {
    private String baseUrl;
    private String apiKey;
    private String organizationId;
    private String projectId;
    private String modelName;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double presencePenalty;
    private Double frequencyPenalty;
    private String responseFormat;
    private Boolean strictJsonSchema;
    private Integer seed;
    private String user;
    private Duration timeout;
    private Integer maxRetries;
    private Boolean logRequests;
    private Boolean logResponses;

    private ChatModelPreferences() {}

    public static ChatModelPreferences.Builder builder() {
      return new ChatModelPreferences.Builder();
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public String getOrganizationId() {
      return organizationId;
    }

    public String getProjectId() {
      return projectId;
    }

    public String getModelName() {
      return modelName;
    }

    public Double getTemperature() {
      return temperature;
    }

    public Double getTopP() {
      return topP;
    }

    public Integer getMaxTokens() {
      return maxTokens;
    }

    public Integer getMaxCompletionTokens() {
      return maxCompletionTokens;
    }

    public Double getPresencePenalty() {
      return presencePenalty;
    }

    public Double getFrequencyPenalty() {
      return frequencyPenalty;
    }

    public String getResponseFormat() {
      return responseFormat;
    }

    public Boolean getStrictJsonSchema() {
      return strictJsonSchema;
    }

    public Integer getSeed() {
      return seed;
    }

    public String getUser() {
      return user;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public Integer getMaxRetries() {
      return maxRetries;
    }

    public Boolean getLogRequests() {
      return logRequests;
    }

    public Boolean getLogResponses() {
      return logResponses;
    }

    @Override
    public String toString() {
      return "Builder{"
          + "baseUrl='"
          + baseUrl
          + '\''
          + ", apiKey='"
          + apiKey
          + '\''
          + ", organizationId='"
          + organizationId
          + '\''
          + ", projectId='"
          + projectId
          + '\''
          + ", modelName='"
          + modelName
          + '\''
          + ", temperature="
          + temperature
          + ", topP="
          + topP
          + ", maxTokens="
          + maxTokens
          + ", maxCompletionTokens="
          + maxCompletionTokens
          + ", presencePenalty="
          + presencePenalty
          + ", frequencyPenalty="
          + frequencyPenalty
          + ", responseFormat='"
          + responseFormat
          + '\''
          + ", strictJsonSchema="
          + strictJsonSchema
          + ", seed="
          + seed
          + ", user='"
          + user
          + '\''
          + ", timeout="
          + timeout
          + ", maxRetries="
          + maxRetries
          + ", logRequests="
          + logRequests
          + ", logResponses="
          + logResponses
          + '}';
    }

    public static class Builder {
      private String baseUrl;
      private String apiKey;
      private String organizationId;
      private String projectId;
      private String modelName;
      private Double temperature;
      private Double topP;
      private Integer maxTokens;
      private Integer maxCompletionTokens;
      private Double presencePenalty;
      private Double frequencyPenalty;
      private String responseFormat;
      private Boolean strictJsonSchema;
      private Integer seed;
      private String user;
      private Duration timeout;
      private Integer maxRetries;
      private Boolean logRequests;
      private Boolean logResponses;

      public Builder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
      }

      public Builder apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
      }

      public Builder organizationId(String organizationId) {
        this.organizationId = organizationId;
        return this;
      }

      public Builder projectId(String projectId) {
        this.projectId = projectId;
        return this;
      }

      public Builder modelName(String modelName) {
        this.modelName = modelName;
        return this;
      }

      public Builder temperature(Double temperature) {
        this.temperature = temperature;
        return this;
      }

      public Builder topP(Double topP) {
        this.topP = topP;
        return this;
      }

      public Builder maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
      }

      public Builder maxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
        return this;
      }

      public Builder presencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
      }

      public Builder frequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
      }

      public Builder responseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
        return this;
      }

      public Builder strictJsonSchema(Boolean strictJsonSchema) {
        this.strictJsonSchema = strictJsonSchema;
        return this;
      }

      public Builder seed(Integer seed) {
        this.seed = seed;
        return this;
      }

      public Builder user(String user) {
        this.user = user;
        return this;
      }

      public Builder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
      }

      public Builder maxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
        return this;
      }

      public Builder logRequests(Boolean logRequests) {
        this.logRequests = logRequests;
        return this;
      }

      public Builder logResponses(Boolean logResponses) {
        this.logResponses = logResponses;
        return this;
      }

      public ChatModelPreferences build() {
        ChatModelPreferences preferences = new ChatModelPreferences();
        preferences.baseUrl = this.baseUrl;
        preferences.apiKey = this.apiKey;
        preferences.organizationId = this.organizationId;
        preferences.projectId = this.projectId;
        preferences.modelName = this.modelName;
        preferences.temperature = this.temperature;
        preferences.topP = this.topP;
        preferences.maxTokens = this.maxTokens;
        preferences.maxCompletionTokens = this.maxCompletionTokens;
        preferences.presencePenalty = this.presencePenalty;
        preferences.frequencyPenalty = this.frequencyPenalty;
        preferences.responseFormat = this.responseFormat;
        preferences.strictJsonSchema = this.strictJsonSchema;
        preferences.seed = this.seed;
        preferences.user = this.user;
        preferences.timeout = this.timeout;
        preferences.maxRetries = this.maxRetries;
        preferences.logRequests = this.logRequests;
        preferences.logResponses = this.logResponses;
        return preferences;
      }
    }
  }

  public static class ChatModelRequest {

    private List<String> userMessages;
    private List<String> systemMessages;

    private ChatModelRequest() {}

    public List<String> getUserMessages() {
      return userMessages;
    }

    public List<String> getSystemMessages() {
      return systemMessages;
    }

    public static ChatModelRequest.Builder builder() {
      return new ChatModelRequest.Builder();
    }

    @Override
    public String toString() {
      return "ChatModelRequest{"
          + "userMessages="
          + String.join(",", userMessages)
          + ", systemMessages="
          + String.join(",", systemMessages)
          + '}';
    }

    public static class Builder {
      private List<String> userMessages = new ArrayList<>();
      private List<String> systemMessages = new ArrayList<>();

      private Builder() {}

      public Builder userMessage(String userMessage) {
        this.userMessages.add(userMessage);
        return this;
      }

      public Builder userMessages(Collection<String> userMessages) {
        this.userMessages.addAll(userMessages);
        return this;
      }

      public Builder systemMessage(String systemMessage) {
        this.systemMessages.add(systemMessage);
        return this;
      }

      public Builder systemMessages(Collection<String> systemMessages) {
        this.systemMessages.addAll(systemMessages);
        return this;
      }

      public ChatModelRequest build() {
        ChatModelRequest request = new ChatModelRequest();
        request.userMessages = this.userMessages;
        request.systemMessages = this.systemMessages;
        return request;
      }
    }
  }
}
