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

package io.serverlessworkflow.impl.executors.http.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BiFunction;

public class TokenResponseHandler implements BiFunction<HttpRequest, TaskContext, JsonNode> {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  @Override
  public JsonNode apply(HttpRequest requestBuilder, TaskContext context) {
    HttpResponse<String> response;
    try {
      response = CLIENT.send(requestBuilder, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new WorkflowException(
            WorkflowError.communication(
                    response.statusCode(),
                    context,
                    "Failed to obtain token: HTTP "
                        + response.statusCode()
                        + " — "
                        + response.body())
                .build());
      }
    } catch (java.net.ConnectException e) {
      throw new RuntimeException("Connection refused: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new RuntimeException("Unable to send request: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Unable to send request: " + e.getMessage(), e);
    }

    try {
      return MAPPER.readTree(response.body());
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON response: " + e.getMessage(), e);
    }
  }
}
