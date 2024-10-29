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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.HTTPArguments;
import io.serverlessworkflow.api.types.WithHTTPHeaders;
import io.serverlessworkflow.api.types.WithHTTPQuery;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Map.Entry;

public class HttpExecutor extends AbstractTaskExecutor<CallHTTP> {

  private static final Client client = ClientBuilder.newClient();

  public HttpExecutor(CallHTTP task) {
    super(task);
  }

  @Override
  protected JsonNode internalExecute(JsonNode node) {
    HTTPArguments httpArgs = task.getWith();
    // missing checks
    String uri =
        httpArgs
            .getEndpoint()
            .getEndpointConfiguration()
            .getUri()
            .getLiteralEndpointURI()
            .getLiteralUriTemplate();
    WebTarget target = client.target(uri);
    WithHTTPQuery query = httpArgs.getQuery();
    if (query != null) {
      for (Entry<String, Object> entry : query.getAdditionalProperties().entrySet()) {
        target = target.queryParam(entry.getKey(), entry.getValue());
      }
    }
    Builder request =
        target
            .resolveTemplates(
                JsonUtils.mapper().convertValue(node, new TypeReference<Map<String, Object>>() {}))
            .request(MediaType.APPLICATION_JSON);
    WithHTTPHeaders headers = httpArgs.getHeaders();
    if (headers != null) {
      headers.getAdditionalProperties().forEach(request::header);
    }
    switch (httpArgs.getMethod().toLowerCase()) {
      case "get":
      default:
        return request.get(JsonNode.class);
      case "post":
        return request.post(Entity.json(httpArgs.getBody()), JsonNode.class);
    }
  }
}
