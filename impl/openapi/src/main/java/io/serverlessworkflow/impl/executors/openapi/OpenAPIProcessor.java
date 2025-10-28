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
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.Set;

class OpenAPIProcessor {

  private final String operationId;

  OpenAPIProcessor(String operationId) {
    this.operationId = operationId;
  }

  public OperationDefinition parse(String content) {
    OpenAPIV3Parser parser = new OpenAPIV3Parser();
    ParseOptions opts = new ParseOptions();
    opts.setResolve(true);
    opts.setResolveFully(false);
    return getOperation(parser.readContents(content).getOpenAPI());
  }

  private OperationDefinition getOperation(OpenAPI openAPI) {
    if (openAPI == null || openAPI.getPaths() == null) {
      throw new IllegalArgumentException("Invalid OpenAPI document");
    }

    Set<String> paths = openAPI.getPaths().keySet();

    for (String path : paths) {
      PathItem pathItem = openAPI.getPaths().get(path);
      OperationAndMethod operationAndMethod = findInPathItem(pathItem, operationId);
      if (operationAndMethod != null) {
        return new OperationDefinition(
            openAPI, operationAndMethod.operation, path, operationAndMethod.method);
      }
    }
    throw new IllegalArgumentException(
        "No operation with id '" + operationId + "' found in OpenAPI document");
  }

  private OperationAndMethod findInPathItem(PathItem pathItem, String operationId) {
    if (pathItem == null) {
      return null;
    }

    if (matches(pathItem.getGet(), operationId))
      return new OperationAndMethod(pathItem.getGet(), "GET");
    if (matches(pathItem.getPost(), operationId))
      return new OperationAndMethod(pathItem.getPost(), "POST");
    if (matches(pathItem.getPut(), operationId))
      return new OperationAndMethod(pathItem.getPut(), "PUT");
    if (matches(pathItem.getDelete(), operationId))
      return new OperationAndMethod(pathItem.getDelete(), "DELETE");
    if (matches(pathItem.getPatch(), operationId))
      return new OperationAndMethod(pathItem.getPatch(), "PATCH");
    if (matches(pathItem.getHead(), operationId))
      return new OperationAndMethod(pathItem.getHead(), "HEAD");
    if (matches(pathItem.getOptions(), operationId))
      return new OperationAndMethod(pathItem.getOptions(), "OPTIONS");
    if (matches(pathItem.getTrace(), operationId))
      return new OperationAndMethod(pathItem.getTrace(), "TRACE");

    return null;
  }

  private boolean matches(Operation op, String operationId) {
    return op != null && operationId.equals(op.getOperationId());
  }

  private record OperationAndMethod(Operation operation, String method) {}
}
