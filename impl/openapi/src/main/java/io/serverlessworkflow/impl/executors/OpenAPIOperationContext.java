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

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;

public record OpenAPIOperationContext(
    String operationId, String path, PathItem.HttpMethod httpMethod, Operation operation) {

  public String httpMethodName() {
    return httpMethod.name();
  }

  public String buildPath(Map<String, Object> replacements) {
    String finalPath = path;
    for (Parameter parameter : operation.getParameters()) {
      if ("path".equals(parameter.getIn())) {
        String name = parameter.getName();
        Object value = replacements.get(name);
        if (value != null) {
          finalPath = path.replaceAll("\\{\\s*" + name + "\\s*}", value.toString());
        }
      }
    }
    return finalPath;
  }

  public MultivaluedMap<String, Object> buildQueryParams(Map<String, Object> replacements) {
    MultivaluedMap<String, Object> queryParams = new MultivaluedHashMap<>();
    for (Parameter parameter : operation.getParameters()) {
      if ("query".equals(parameter.getIn())) {
        String name = parameter.getName();
        Object value = replacements.get(name);
        if (value != null) {
          queryParams.add(name, value.toString());
        }
      }
    }
    return queryParams;
  }
}
