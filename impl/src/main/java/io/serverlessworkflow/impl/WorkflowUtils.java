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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.types.ExportAs;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.resources.StaticResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

public class WorkflowUtils {

  private WorkflowUtils() {}

  public static Optional<SchemaValidator> getSchemaValidator(
      SchemaValidatorFactory validatorFactory, Optional<JsonNode> node) {
    return node.map(n -> validatorFactory.getValidator(n));
  }

  public static Optional<JsonNode> schemaToNode(WorkflowFactories factories, SchemaUnion schema) {
    if (schema != null) {
      if (schema.getSchemaInline() != null) {
        SchemaInline inline = schema.getSchemaInline();
        return Optional.of(JsonUtils.mapper().convertValue(inline.getDocument(), JsonNode.class));
      } else if (schema.getSchemaExternal() != null) {
        SchemaExternal external = schema.getSchemaExternal();
        StaticResource resource = factories.getResourceLoader().loadStatic(external.getResource());
        ObjectMapper mapper = WorkflowFormat.fromFileName(resource.name()).mapper();
        try (InputStream in = resource.open()) {
          return Optional.of(mapper.readTree(in));
        } catch (IOException io) {
          throw new UncheckedIOException(io);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      ExpressionFactory exprFactory, InputFrom from) {
    return from != null
        ? Optional.of(buildWorkflowFilter(exprFactory, from.getString(), from.getObject()))
        : Optional.empty();
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      ExpressionFactory exprFactory, OutputAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(exprFactory, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      ExpressionFactory exprFactory, ExportAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(exprFactory, as.getString(), as.getObject()))
        : Optional.empty();
  }

  private static WorkflowFilter buildWorkflowFilter(
      ExpressionFactory exprFactory, String str, Object object) {
    if (str != null) {
      Expression expression = exprFactory.getExpression(str);
      return expression::eval;
    } else {
      Object exprObj = ExpressionUtils.buildExpressionObject(object, exprFactory);
      return exprObj instanceof Map
          ? (w, t, n) ->
              JsonUtils.fromValue(
                  ExpressionUtils.evaluateExpressionMap((Map<String, Object>) exprObj, w, t, n))
          : (w, t, n) -> JsonUtils.fromValue(object);
    }
  }
}
