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
package io.serverlessworkflow.impl.expressions;

import com.fasterxml.jackson.databind.JsonNode;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.util.Map;
import java.util.Optional;

public class ExpressionUtils {

  private static final String EXPR_PREFIX = "${";
  private static final String EXPR_SUFFIX = "}";

  private ExpressionUtils() {}

  public static Map<String, Object> buildExpressionMap(
      Map<String, Object> origMap, ExpressionFactory factory) {
    return new ProxyMap(origMap, o -> isExpr(o) ? factory.getExpression(o.toString()) : o);
  }

  public static Map<String, Object> evaluateExpressionMap(
      Map<String, Object> origMap,
      WorkflowContext workflow,
      Optional<TaskContext<?>> task,
      JsonNode n) {
    return new ProxyMap(
        origMap,
        o ->
            o instanceof Expression
                ? JsonUtils.toJavaValue(((Expression) o).eval(workflow, task, n))
                : o);
  }

  public static Object buildExpressionObject(Object obj, ExpressionFactory factory) {
    return obj instanceof Map
        ? ExpressionUtils.buildExpressionMap((Map<String, Object>) obj, factory)
        : obj;
  }

  public static Object evaluateExpressionObject(
      Object obj, WorkflowContext workflow, Optional<TaskContext<?>> task, JsonNode node) {
    return obj instanceof Map
        ? ExpressionUtils.evaluateExpressionMap((Map<String, Object>) obj, workflow, task, node)
        : obj;
  }

  public static boolean isExpr(Object expr) {
    return expr instanceof String && ((String) expr).startsWith(EXPR_PREFIX);
  }

  public static String trimExpr(String expr) {
    expr = expr.trim();
    if (expr.startsWith(EXPR_PREFIX)) {
      expr = trimExpr(expr, EXPR_PREFIX, EXPR_SUFFIX);
    }
    return expr.trim();
  }

  private static String trimExpr(String expr, String prefix, String suffix) {
    expr = expr.substring(prefix.length());
    if (expr.endsWith(suffix)) {
      expr = expr.substring(0, expr.length() - suffix.length());
    }
    return expr;
  }
}
