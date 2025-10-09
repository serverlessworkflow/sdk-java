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
import io.serverlessworkflow.impl.expressions.ExpressionDescriptor;
import io.serverlessworkflow.impl.expressions.ExpressionUtils;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import io.serverlessworkflow.impl.schema.SchemaValidator;
import io.serverlessworkflow.impl.schema.SchemaValidatorFactory;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowUtils {

  private WorkflowUtils() {}

  private static final Logger logger = LoggerFactory.getLogger(WorkflowUtils.class);

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

  public static Optional<WorkflowFilter> buildWorkflowFilter(WorkflowApplication app, ExportAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(app, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static WorkflowValueResolver<String> buildStringFilter(
      WorkflowApplication app, String expression, String literal) {
    return expression != null ? toExprString(app, expression) : toString(literal);
  }

  public static WorkflowValueResolver<String> buildStringFilter(
      WorkflowApplication app, String str) {
    return ExpressionUtils.isExpr(str) ? toExprString(app, str) : toString(str);
  }

  public static WorkflowValueResolver<String> buildCollectionFilter(
      WorkflowApplication app, String expression) {
    return expression != null ? toExprString(app, expression) : toString(expression);
  }

  private static WorkflowValueResolver<String> toExprString(
      WorkflowApplication app, String expression) {
    return app.expressionFactory().resolveString(ExpressionDescriptor.from(expression));
  }

  private static WorkflowValueResolver<String> toString(String literal) {
    return (w, t, m) -> literal;
  }

  public static WorkflowFilter buildWorkflowFilter(
      WorkflowApplication app, String str, Object object) {
    return app.expressionFactory()
        .buildFilter(new ExpressionDescriptor(str, object), app.modelFactory());
  }

  public static WorkflowValueResolver<String> buildStringResolver(
      WorkflowApplication app, String str) {
    return app.expressionFactory().resolveString(ExpressionDescriptor.from(str));
  }

  public static WorkflowValueResolver<String> buildStringResolver(
      WorkflowApplication app, String str, Object obj) {
    return app.expressionFactory().resolveString(new ExpressionDescriptor(str, obj));
  }

  public static WorkflowValueResolver<Map<String, Object>> buildMapResolver(
      WorkflowApplication app, String str, Object obj) {
    return app.expressionFactory().resolveMap(new ExpressionDescriptor(str, obj));
  }

  public static WorkflowFilter buildWorkflowFilter(WorkflowApplication app, String str) {
    return app.expressionFactory().buildFilter(ExpressionDescriptor.from(str), app.modelFactory());
  }

  public static WorkflowPredicate buildPredicate(WorkflowApplication app, String str) {
    return app.expressionFactory().buildPredicate(ExpressionDescriptor.from(str));
  }

  public static Optional<WorkflowFilter> optionalFilter(WorkflowApplication app, String str) {
    return str != null ? Optional.of(buildWorkflowFilter(app, str)) : Optional.empty();
  }

  public static Optional<WorkflowPredicate> optionalPredicate(WorkflowApplication app, String str) {
    return str != null ? Optional.of(buildPredicate(app, str)) : Optional.empty();
  }

  public static Optional<WorkflowFilter> optionalFilter(
      WorkflowApplication app, Object obj, String str) {
    return str != null || obj != null
        ? Optional.of(buildWorkflowFilter(app, str, obj))
        : Optional.empty();
  }

  public static String toString(UriTemplate template) {
    URI uri = template.getLiteralUri();
    return uri != null ? uri.toString() : template.getLiteralUriTemplate();
  }

  public static void safeClose(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ex) {
        logger.warn("Error closing resource {}", closeable.getClass().getName(), ex);
      }
    }
  }
}
