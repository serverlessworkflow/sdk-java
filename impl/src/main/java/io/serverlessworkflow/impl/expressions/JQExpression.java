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
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.internal.tree.FunctionCall;
import net.thisptr.jackson.jq.internal.tree.StringInterpolation;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JQExpression implements Expression {

  private static final Logger logger = LoggerFactory.getLogger(JQExpression.class);
  private final Map<Class<? extends net.thisptr.jackson.jq.Expression>, Collection<Field>>
      declaredFieldsMap = new ConcurrentHashMap<>();
  private final Map<Class<? extends net.thisptr.jackson.jq.Expression>, Collection<Field>>
      allFieldsMap = new ConcurrentHashMap<>();

  private final Supplier<Scope> scope;
  private final String expr;

  private net.thisptr.jackson.jq.Expression internalExpr;
  private static Field rhsField;

  static {
    try {
      rhsField = BinaryOperatorExpression.class.getDeclaredField("rhs");
      rhsField.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      logger.warn("Unexpected exception while resolving rhs field", e);
    }
  }

  public JQExpression(Supplier<Scope> scope, String expr, Version version)
      throws JsonQueryException {
    this.expr = expr;
    this.scope = scope;
    this.internalExpr = compile(version);
    checkFunctionCall(internalExpr);
  }

  private net.thisptr.jackson.jq.Expression compile(Version version) throws JsonQueryException {
    net.thisptr.jackson.jq.Expression expression;
    try {
      expression = ExpressionParser.compile(expr, version);
    } catch (JsonQueryException ex) {
      expression = handleStringInterpolation(version).orElseThrow(() -> ex);
    }
    checkFunctionCall(expression);
    return expression;
  }

  private Optional<net.thisptr.jackson.jq.Expression> handleStringInterpolation(Version version) {
    if (!expr.startsWith("\"")) {
      try {
        net.thisptr.jackson.jq.Expression expression =
            ExpressionParser.compile("\"" + expr + "\"", version);
        if (expression instanceof StringInterpolation) {
          return Optional.of(expression);
        }
      } catch (JsonQueryException ex) {
        // ignoring it
      }
    }
    return Optional.empty();
  }

  private interface TypedOutput<T> extends Output {
    T getResult();
  }

  @SuppressWarnings("unchecked")
  private <T> TypedOutput<T> output(Class<T> returnClass) {
    TypedOutput<T> out;
    if (String.class.isAssignableFrom(returnClass)) {
      out = (TypedOutput<T>) new StringOutput();
    } else if (Collection.class.isAssignableFrom(returnClass)) {
      out = (TypedOutput<T>) new CollectionOutput();
    } else {
      out = (TypedOutput<T>) new JsonNodeOutput();
    }
    return out;
  }

  private static class StringOutput implements TypedOutput<String> {
    StringBuilder sb = new StringBuilder();

    @Override
    public void emit(JsonNode out) throws JsonQueryException {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      if (!out.isNull() && out.asText() != null) {
        sb.append(out.asText());
      }
    }

    @Override
    public String getResult() {
      return sb.toString();
    }
  }

  private static class CollectionOutput implements TypedOutput<Collection<Object>> {
    Collection<Object> result = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public void emit(JsonNode out) throws JsonQueryException {
      Object obj = JsonUtils.toJavaValue(out);
      if (obj instanceof Collection) result.addAll((Collection<Object>) obj);
      else {
        result.add(obj);
      }
    }

    @Override
    public Collection<Object> getResult() {
      return result;
    }
  }

  private static class JsonNodeOutput implements TypedOutput<JsonNode> {

    private JsonNode result;
    private boolean arrayCreated;

    @Override
    public void emit(JsonNode out) throws JsonQueryException {
      if (this.result == null) {
        this.result = out;
      } else if (!arrayCreated) {
        ArrayNode newNode = JsonUtils.mapper().createArrayNode();
        newNode.add(this.result).add(out);
        this.result = newNode;
        arrayCreated = true;
      } else {
        ((ArrayNode) this.result).add(out);
      }
    }

    @Override
    public JsonNode getResult() {
      return result;
    }
  }

  @Override
  public JsonNode eval(
      WorkflowContext workflow, TaskContext<? extends TaskBase> task, JsonNode node) {
    TypedOutput<JsonNode> output = output(JsonNode.class);
    try {
      internalExpr.apply(this.scope.get(), node, output);
      return output.getResult();
    } catch (JsonQueryException e) {
      throw new IllegalArgumentException(
          "Unable to evaluate content " + node + " using expr " + expr, e);
    }
  }

  private void checkFunctionCall(net.thisptr.jackson.jq.Expression toCheck)
      throws JsonQueryException {
    if (toCheck instanceof FunctionCall) {
      toCheck.apply(scope.get(), JsonUtils.mapper().createObjectNode(), out -> {});
    } else if (toCheck instanceof BinaryOperatorExpression) {
      if (rhsField != null) {
        try {
          checkFunctionCall((net.thisptr.jackson.jq.Expression) rhsField.get(toCheck));
        } catch (ReflectiveOperationException e) {
          logger.warn(
              "Ignoring unexpected error {} while accesing field {} for class{} and expression {}",
              e.getMessage(),
              rhsField.getName(),
              toCheck.getClass(),
              expr);
        }
      }
    } else if (toCheck != null) {
      for (Field f : getAllExprFields(toCheck))
        try {
          checkFunctionCall((net.thisptr.jackson.jq.Expression) f.get(toCheck));
        } catch (ReflectiveOperationException e) {
          logger.warn(
              "Ignoring unexpected error {} while accesing field {} for class{} and expression {}",
              e.getMessage(),
              f.getName(),
              toCheck.getClass(),
              expr);
        }
    }
  }

  private Collection<Field> getAllExprFields(net.thisptr.jackson.jq.Expression toCheck) {
    return allFieldsMap.computeIfAbsent(toCheck.getClass(), this::getAllExprFields);
  }

  private Collection<Field> getAllExprFields(
      Class<? extends net.thisptr.jackson.jq.Expression> clazz) {
    Collection<Field> fields = new HashSet<>();
    Class<?> currentClass = clazz;
    do {
      fields.addAll(
          declaredFieldsMap.computeIfAbsent(
              currentClass.asSubclass(net.thisptr.jackson.jq.Expression.class),
              this::getDeclaredExprFields));
      currentClass = currentClass.getSuperclass();
    } while (net.thisptr.jackson.jq.Expression.class.isAssignableFrom(currentClass));
    return fields;
  }

  private Collection<Field> getDeclaredExprFields(
      Class<? extends net.thisptr.jackson.jq.Expression> clazz) {
    Collection<Field> fields = new HashSet<>();
    for (Field f : clazz.getDeclaredFields()) {
      if (net.thisptr.jackson.jq.Expression.class.isAssignableFrom(f.getType())) {
        f.setAccessible(true);
        fields.add(f);
      }
    }
    return fields;
  }
}
