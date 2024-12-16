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
package io.serverlessworkflow.impl.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OpenAPIReader {

  private static final String HTTPS = "https";
  private static final String HTTP = "http";
  private static final String DEFAULT_SCHEME = HTTPS;
  private static final Set<String> ALLOWED_SCHEMES = Set.of(HTTPS, HTTP);

  public static String getHost(JsonNode jsonNode) {
    JsonNode host = jsonNode.get("host");
    if (host == null) {
      return null;
    }
    String scheme = getScheme(jsonNode);
    return scheme + "://" + host.asText();
  }

  private static String getScheme(JsonNode jsonNode) {
    ArrayNode array = jsonNode.withArrayProperty("schemes");
    if (array != null && !array.isEmpty()) {
      String firstScheme = array.get(0).asText();
      return ALLOWED_SCHEMES.contains(firstScheme) ? firstScheme : DEFAULT_SCHEME;
    }
    return DEFAULT_SCHEME;
  }

  public static Optional<JsonNode> readOperation(JsonNode jsonNode, String operationId) {
    JsonNode paths = jsonNode.get("paths");
    for (Map.Entry<String, JsonNode> entry : paths.properties()) {
      for (Map.Entry<String, JsonNode> httpMethod : entry.getValue().properties()) {
        if (httpMethod.getValue().get("operationId").asText().equals(operationId)) {
          return Optional.ofNullable(httpMethod.getValue());
        }
      }
    }
    return Optional.empty();
  }
}
