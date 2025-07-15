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

import io.serverlessworkflow.api.types.ExportAs;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.SchemaUnion;
import io.serverlessworkflow.api.types.UriTemplate;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.jsonschema.SchemaValidator;
import io.serverlessworkflow.impl.jsonschema.SchemaValidatorFactory;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

public class WorkflowUtils {

  private WorkflowUtils() {}

  public static Optional<SchemaValidator> getSchemaValidator(
      SchemaValidatorFactory validatorFactory, ResourceLoader resourceLoader, SchemaUnion schema) {
    if (schema != null) {

      if (schema.getSchemaInline() != null) {
        return Optional.of(validatorFactory.getValidator(schema.getSchemaInline()));
      } else if (schema.getSchemaExternal() != null) {
        return Optional.of(
            validatorFactory.getValidator(
                resourceLoader.loadStatic(schema.getSchemaExternal().getResource())));
      }
    }
    return Optional.empty();
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      WorkflowApplication app, InputFrom from) {
    return from != null
        ? Optional.of(buildWorkflowFilter(app, from.getString(), from.getObject()))
        : Optional.empty();
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(WorkflowApplication app, OutputAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(app, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static <T> ExpressionHolder<T> buildExpressionHolder(
      WorkflowApplication app, String expression, T literal, Function<WorkflowModel, T> converter) {
    return expression != null
        ? buildExpressionHolder(buildWorkflowFilter(app, expression), converter)
        : buildExpressionHolder(literal);
  }

  private static <T> ExpressionHolder<T> buildExpressionHolder(
      WorkflowFilter filter, Function<WorkflowModel, T> converter) {
    return (w, t) -> converter.apply(filter.apply(w, t, t.input()));
  }

  private static <T> ExpressionHolder<T> buildExpressionHolder(T literal) {
    return (w, t) -> literal;
  }

  public static Optional<WorkflowFilter> buildWorkflowFilter(WorkflowApplication app, ExportAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(app, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static StringFilter buildStringFilter(
      WorkflowApplication app, String expression, String literal) {
    return expression != null ? toString(buildWorkflowFilter(app, expression)) : toString(literal);
  }

  public static StringFilter buildStringFilter(WorkflowApplication app, String str) {
    return ExpressionUtils.isExpr(str) ? toString(buildWorkflowFilter(app, str)) : toString(str);
  }

  private static StringFilter toString(WorkflowFilter filter) {
    return (w, t) ->
        filter
            .apply(w, t, t.input())
            .asText()
            .orElseThrow(() -> new IllegalArgumentException("Result is not an string"));
  }

  private static StringFilter toString(String literal) {
    return (w, t) -> literal;
  }

  public static WorkflowFilter buildWorkflowFilter(
      WorkflowApplication app, String str, Object object) {
    return app.expressionFactory().buildFilter(str, object);
  }

  public static WorkflowFilter buildWorkflowFilter(WorkflowApplication app, String str) {
    return app.expressionFactory().buildFilter(str, null);
  }

  public static Optional<WorkflowFilter> optionalFilter(WorkflowApplication app, String str) {
    return str != null ? Optional.of(buildWorkflowFilter(app, str)) : Optional.empty();
  }

  public static String toString(UriTemplate template) {
    URI uri = template.getLiteralUri();
    return uri != null ? uri.toString() : template.getLiteralUriTemplate();
  }
}
