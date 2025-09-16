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
package io.serverlessworkflow.impl.executors.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;

public class OperationDefinition {
  private final Operation operation;
  private final String method;
  private final OpenAPI openAPI;
  private final String path;

  public OperationDefinition(OpenAPI openAPI, Operation operation, String path, String method) {
    this.openAPI = openAPI;
    this.operation = operation;
    this.path = path;
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public Operation getOperation() {
    return operation;
  }

  public List<String> getServers() {
    return openAPI.getServers().stream().map(Server::getUrl).toList();
  }

  public List<Parameter> getParameters() {
    if (operation.getParameters() == null) {
      return List.of();
    }
    return operation.getParameters();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public Map<String, Schema> getBody() {
    if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
      Content content = operation.getRequestBody().getContent();
      if (content.containsKey("application/json")) {
        MediaType mt = content.get("application/json");
        if (mt.getSchema().get$ref() != null && !mt.getSchema().get$ref().isEmpty()) {
          Schema<?> schema = resolveSchema(mt.getSchema().get$ref());
          return schema.getProperties();
        } else if (mt.getSchema().getProperties() != null) {
          return mt.getSchema().getProperties();
        } else {
          throw new IllegalArgumentException(
              "Can't resolve schema for request body of operation " + operation.getOperationId());
        }
      } else {
        throw new IllegalArgumentException("Only 'application/json' content type is supported");
      }
    }
    return Map.of();
  }

  public String getContentType() {
    String method = getMethod().toUpperCase();

    if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
      if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
        Content content = operation.getRequestBody().getContent();
        if (!content.isEmpty()) {
          return content.keySet().iterator().next();
        }
      }
    }

    if (operation.getResponses() != null) {
      for (String code : new String[] {"200", "201", "204"}) {
        ApiResponse resp = operation.getResponses().get(code);
        if (resp != null && resp.getContent() != null && !resp.getContent().isEmpty()) {
          return resp.getContent().keySet().iterator().next();
        }
      }
      for (Map.Entry<String, ApiResponse> e : operation.getResponses().entrySet()) {
        Content content = e.getValue().getContent();
        if (content != null && !content.isEmpty()) {
          return content.keySet().iterator().next();
        }
      }
    }

    throw new IllegalStateException(
        "No content type found for operation " + operation.getOperationId() + " [" + method + "]");
  }

  public Schema<?> resolveSchema(String ref) {
    if (ref == null || !ref.startsWith("#/components/schemas/")) {
      throw new IllegalArgumentException("Unsupported $ref format: " + ref);
    }
    String name = ref.substring("#/components/schemas/".length());
    if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
      throw new IllegalStateException("No components/schemas found in OpenAPI");
    }
    Schema<?> schema = openAPI.getComponents().getSchemas().get(name);
    if (schema == null) {
      throw new IllegalArgumentException("Schema not found: " + name);
    }
    return schema;
  }
}
