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

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.V;
import io.serverlessworkflow.api.types.ai.CallAILangChainChatModel;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallAILangChainChatModelExecutor implements AIChatModelExecutor {

  private final CallAILangChainChatModel callAIChatModel;

  public CallAILangChainChatModelExecutor(CallAILangChainChatModel callAIChatModel) {
    this.callAIChatModel = callAIChatModel;
  }

  @Override
  public Object apply(Object javaObject) {
    ChatModel chatModel = callAIChatModel.getChatModel();
    Class<?> chatModelRequest = callAIChatModel.getChatModelRequest();
    Map<String, Object> substitutions = (Map<String, Object>) javaObject;
    validate(chatModel, chatModelRequest, substitutions);

    Method method = getMethod(chatModelRequest, callAIChatModel.getMethodName());
    List<String> resolvedParameters = resolvedParameters(method, substitutions);

    var aiServices = AiServices.builder(chatModelRequest).chatModel(chatModel).build();
    try {
      Object[] args = new Object[resolvedParameters.size()];
      for (int i = 0; i < resolvedParameters.size(); i++) {
        String paramName = resolvedParameters.get(i);
        args[i] = substitutions.get(paramName);
      }

      Object response = method.invoke(aiServices, args);
      substitutions.put("result", response);
    } catch (Exception e) {
      throw new RuntimeException("Error invoking chat model method", e);
    }
    return substitutions;
  }

  private void validate(
      ChatModel chatModel, Class<?> chatModelRequest, Map<String, Object> substitutions) {}

  private Method getMethod(Class<?> chatModelRequest, String methodName) {
    for (Method method : chatModelRequest.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    throw new IllegalArgumentException(
        "Method " + methodName + " not found in class " + chatModelRequest.getName());
  }

  private List<String> resolvedParameters(Method method, Map<String, Object> substitutions) {
    List<String> resolvedParameters = new ArrayList<>();
    for (Parameter parameter : method.getParameters()) {
      if (parameter.getAnnotation(V.class) != null) {
        V v = parameter.getAnnotation(V.class);
        String paramName = v.value();
        if (substitutions.containsKey(paramName)) {
          resolvedParameters.add(paramName);
        } else {
          throw new IllegalArgumentException("Missing substitution for parameter: " + paramName);
        }
      }
    }
    return resolvedParameters;
  }
}
