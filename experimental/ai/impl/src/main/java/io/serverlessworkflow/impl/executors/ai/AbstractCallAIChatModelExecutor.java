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
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Map;

public abstract class AbstractCallAIChatModelExecutor<T> {

  public abstract Object apply(T callAIChatModel, Object javaObject);

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
      responseMap.put("text", aiMessage.text());
    }

    return responseMap;
  }
}
