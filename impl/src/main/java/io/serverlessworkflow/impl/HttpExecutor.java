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
package io.serverlessworkflow.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.HTTPArguments;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HttpExecutor extends AbstractTaskExecutor<CallHTTP> {

  public HttpExecutor(CallHTTP task) {
    super(task);
  }

  @Override
  protected JsonNode internalExecute(JsonNode node) {
    try {
      HTTPArguments httpArgs = task.getWith();
      // todo think on how to solve this oneOf in an smarter way
      // URL url = new URL(((Endpoint) httpArgs.getEndpoint()).getUri().toString());
      URL url = new URL(((Map) httpArgs.getEndpoint()).get("uri").toString());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod(httpArgs.getMethod().toUpperCase());
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
          return JsonUtils.mapper().readValue(in, JsonNode.class);
        }
      }
      throw new IllegalArgumentException("Respose code is " + responseCode);

    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
