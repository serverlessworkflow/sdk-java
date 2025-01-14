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
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.ExpressionFactory;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.json.JsonUtils;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import io.serverlessworkflow.impl.resources.StaticResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class WorkflowUtils {

  private WorkflowUtils() {}

  public static Optional<SchemaValidator> getSchemaValidator(
      SchemaValidatorFactory validatorFactory, ResourceLoader resourceLoader, SchemaUnion schema) {
    return schemaToNode(resourceLoader, schema).map(n -> validatorFactory.getValidator(n));
  }

  private static Optional<JsonNode> schemaToNode(
      ResourceLoader resourceLoader, SchemaUnion schema) {
    if (schema != null) {
      if (schema.getSchemaInline() != null) {
        SchemaInline inline = schema.getSchemaInline();
        return Optional.of(JsonUtils.mapper().convertValue(inline.getDocument(), JsonNode.class));
      } else if (schema.getSchemaExternal() != null) {
        SchemaExternal external = schema.getSchemaExternal();
        StaticResource resource = resourceLoader.loadStatic(external.getResource());
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

  public static <T> ExpressionHolder<T> buildExpressionHolder(
      ExpressionFactory exprFactory,
      String expression,
      T literal,
      Function<JsonNode, T> converter) {
    return expression != null
        ? buildExpressionHolder(buildWorkflowFilter(exprFactory, expression), converter)
        : buildExpressionHolder(literal);
  }

  private static <T> ExpressionHolder<T> buildExpressionHolder(
      WorkflowFilter filter, Function<JsonNode, T> converter) {
    return (w, t) -> converter.apply(filter.apply(w, t, t.input()));
  }

  private static <T> ExpressionHolder<T> buildExpressionHolder(T literal) {
    return (w, t) -> literal;
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      ExpressionFactory exprFactory, ExportAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(exprFactory, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static StringFilter buildStringFilter(
      ExpressionFactory exprFactory, String expression, String literal) {
    return expression != null
        ? toString(buildWorkflowFilter(exprFactory, expression))
        : toString(literal);
  }

  public static StringFilter buildStringFilter(ExpressionFactory exprFactory, String str) {
    return ExpressionUtils.isExpr(str)
        ? toString(buildWorkflowFilter(exprFactory, str))
        : toString(str);
  }

  private static StringFilter toString(WorkflowFilter filter) {
    return (w, t) -> filter.apply(w, t, t.input()).asText();
  }

  private static StringFilter toString(String literal) {
    return (w, t) -> literal;
  }

  public static WorkflowFilter buildWorkflowFilter(
      ExpressionFactory exprFactory, String str, Object object) {
    if (str != null) {
      return buildWorkflowFilter(exprFactory, str);
    } else if (object != null) {
      Object exprObj = ExpressionUtils.buildExpressionObject(object, exprFactory);
      return exprObj instanceof Map
          ? (w, t, n) ->
              JsonUtils.fromValue(
                  ExpressionUtils.evaluateExpressionMap((Map<String, Object>) exprObj, w, t, n))
          : (w, t, n) -> JsonUtils.fromValue(object);
    }
    throw new IllegalStateException("Both object and str are null");
  }

  public static LongFilter buildLongFilter(
      ExpressionFactory exprFactory, String expression, Long literal) {
    return expression != null
        ? toLong(buildWorkflowFilter(exprFactory, expression))
        : toLong(literal);
  }

  private static LongFilter toLong(WorkflowFilter filter) {
    return (w, t) -> filter.apply(w, t, t.input()).asLong();
  }

  private static LongFilter toLong(Long literal) {
    return (w, t) -> literal;
  }

  public static WorkflowFilter buildWorkflowFilter(ExpressionFactory exprFactory, String str) {
    assert str != null;
    Expression expression = exprFactory.getExpression(str);
    return expression::eval;
  }

  public static Optional<WorkflowFilter> optionalFilter(ExpressionFactory exprFactory, String str) {
    return str != null ? Optional.of(buildWorkflowFilter(exprFactory, str)) : Optional.empty();
  }

  public static String toString(UriTemplate template) {
    URI uri = template.getLiteralUri();
    return uri != null ? uri.toString() : template.getLiteralUriTemplate();
  }
}
