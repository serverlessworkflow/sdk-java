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
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OperationDefinition {
  private final Operation operation;
  private final String method;
  private final OpenAPI openAPI;
  private final String path;
  private final boolean emulateSwaggerV2BodyParameters;

  OperationDefinition(
      OpenAPI openAPI,
      Operation operation,
      String path,
      String method,
      boolean emulateSwaggerV2BodyParameters) {
    this.openAPI = openAPI;
    this.operation = operation;
    this.path = path;
    this.method = method;
    this.emulateSwaggerV2BodyParameters = emulateSwaggerV2BodyParameters;
  }

  String getMethod() {
    return method;
  }

  String getPath() {
    return path;
  }

  Operation getOperation() {
    return operation;
  }

  List<String> getServers() {
    if (openAPI.getServers() == null) {
      return List.of();
    }
    return openAPI.getServers().stream().map(Server::getUrl).toList();
  }

  List<ParameterDefinition> getParameters() {
    return emulateSwaggerV2BodyParameters ? getSwaggerV2Parameters() : getOpenApiParameters();
  }

  private List<ParameterDefinition> getOpenApiParameters() {
    if (operation.getParameters() == null) {
      return List.of();
    }
    return operation.getParameters().stream().map(ParameterDefinition::new).toList();
  }

  @SuppressWarnings({"rawtypes"})
  private List<ParameterDefinition> getSwaggerV2Parameters() {
    if (operation.getParameters() != null && !operation.getParameters().isEmpty()) {
      return operation.getParameters().stream().map(ParameterDefinition::new).toList();
    }
    if (operation.getRequestBody() != null) {
      Schema<?> schema = null;
      if (operation.getRequestBody().getContent() != null
          && operation
              .getRequestBody()
              .getContent()
              .containsKey(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)) {
        MediaType mt =
            operation
                .getRequestBody()
                .getContent()
                .get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON);
        schema = mt.getSchema();
      } else if (operation.getRequestBody().get$ref() != null) {
        schema = resolveSchema(operation.getRequestBody().get$ref());
      }

      if (schema == null) {
        return List.of();
      }

      Set<String> required =
          schema.getRequired() != null ? new HashSet<>(schema.getRequired()) : new HashSet<>();

      Map<String, Schema> properties = schema.getProperties();
      if (properties != null) {
        List<ParameterDefinition> result = new ArrayList<>();
        for (Map.Entry<String, Schema> prop : properties.entrySet()) {
          String fieldName = prop.getKey();
          ParameterDefinition fieldParam =
              new ParameterDefinition(
                  fieldName, "body", required.contains(fieldName), prop.getValue());
          result.add(fieldParam);
        }
        return result;
      }
    }
    return List.of();
  }

  Schema<?> resolveSchema(String ref) {
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
