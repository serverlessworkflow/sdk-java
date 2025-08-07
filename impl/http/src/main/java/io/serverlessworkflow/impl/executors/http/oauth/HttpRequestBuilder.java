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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpRequestBuilder {

  private final Map<String, WorkflowValueResolver<String>> headers;

  private final Map<String, WorkflowValueResolver<String>> queryParams;

  private final WorkflowApplication app;

  private URI uri;

  private String method;

  public HttpRequestBuilder(WorkflowApplication app) {
    this.app = app;
    headers = new HashMap<>();
    queryParams = new HashMap<>();
  }

  public HttpRequestBuilder addHeader(String key, String token) {
    headers.put(key, WorkflowUtils.buildStringFilter(app, token));
    return this;
  }

  public HttpRequestBuilder addQueryParam(String key, String token) {
    queryParams.put(key, WorkflowUtils.buildStringFilter(app, token));
    return this;
  }

  public HttpRequestBuilder withUri(URI uri) {
    this.uri = uri;
    return this;
  }

  public HttpRequestBuilder withMethod(String method) {
    this.method = method;
    return this;
  }

  public HttpRequest build(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    HttpRequest.Builder request = HttpRequest.newBuilder();

    for (var entry : headers.entrySet()) {
      String headerValue = entry.getValue().apply(workflow, task, model);
      if (headerValue != null) {
        request = request.header(entry.getKey(), headerValue);
      }
    }

    request.header("Accept", "application/json");

    if (uri == null) {
      throw new IllegalStateException("URI must be set before building the request");
    }

    String encoded =
        queryParams.entrySet().stream()
            .map(
                e -> {
                  String v = e.getValue().apply(workflow, task, model);
                  if (v == null) return null;
                  return URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                      + "="
                      + URLEncoder.encode(v, StandardCharsets.UTF_8);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("&"));

    if (method != null) {
      switch (method.toUpperCase()) {
        case "GET" -> {
          if (!encoded.isEmpty()) {
            String sep = (uri.getQuery() == null || uri.getQuery().isEmpty()) ? "?" : "&";
            uri = URI.create(uri.toString() + sep + encoded);
          }
          request.uri(uri).GET();
        }
        case "POST" -> {
          request.uri(uri);
          HttpRequest.BodyPublisher body =
              encoded.isEmpty()
                  ? HttpRequest.BodyPublishers.noBody()
                  : HttpRequest.BodyPublishers.ofString(encoded);
          request.POST(body);
        }
        default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
      }
    } else {
      throw new IllegalStateException("HTTP method must be set before building the request");
    }
    request.timeout(Duration.ofSeconds(15));
    return request.build();
  }
}
