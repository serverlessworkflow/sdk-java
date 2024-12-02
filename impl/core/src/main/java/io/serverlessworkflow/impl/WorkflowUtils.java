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
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

public class WorkflowUtils {

  private WorkflowUtils() {}

  public static Optional<SchemaValidator> getSchemaValidator(
      SchemaValidatorFactory validatorFactory, Optional<JsonNode> node) {
    return node.map(n -> validatorFactory.getValidator(n));
  }

  public static Optional<JsonNode> schemaToNode(ResourceLoader resourceLoader, SchemaUnion schema) {
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

  public static Optional<WorkflowFilter> buildWorkflowFilter(
      ExpressionFactory exprFactory, ExportAs as) {
    return as != null
        ? Optional.of(buildWorkflowFilter(exprFactory, as.getString(), as.getObject()))
        : Optional.empty();
  }

  public static StringFilter buildStringFilter(
      ExpressionFactory exprFactory, String expression, String literal) {
    return expression != null ? from(buildWorkflowFilter(exprFactory, expression)) : from(literal);
  }

  public static StringFilter buildStringFilter(ExpressionFactory exprFactory, String str) {
    return ExpressionUtils.isExpr(str) ? from(buildWorkflowFilter(exprFactory, str)) : from(str);
  }

  public static StringFilter from(WorkflowFilter filter) {
    return (w, t) -> filter.apply(w, t, t.input()).asText();
  }

  private static StringFilter from(String literal) {
    return (w, t) -> literal;
  }

  private static WorkflowFilter buildWorkflowFilter(
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

  private static TaskItem findTaskByName(ListIterator<TaskItem> iter, String taskName) {
    int currentIndex = iter.nextIndex();
    while (iter.hasPrevious()) {
      TaskItem item = iter.previous();
      if (item.getName().equals(taskName)) {
        return item;
      }
    }
    while (iter.nextIndex() < currentIndex) {
      iter.next();
    }
    while (iter.hasNext()) {
      TaskItem item = iter.next();
      if (item.getName().equals(taskName)) {
        return item;
      }
    }
    throw new IllegalArgumentException("Cannot find task with name " + taskName);
  }

  public static void processTaskList(
      List<TaskItem> tasks, WorkflowContext context, TaskContext<?> parentTask) {
    parentTask.position().addProperty("do");
    TaskContext<? extends TaskBase> currentContext = parentTask;
    if (!tasks.isEmpty()) {
      ListIterator<TaskItem> iter = tasks.listIterator();
      TaskItem nextTask = iter.next();
      while (nextTask != null) {
        TaskItem task = nextTask;
        parentTask.position().addIndex(iter.previousIndex());
        currentContext = executeTask(context, parentTask, task, currentContext.output());
        FlowDirective flowDirective = currentContext.flowDirective();
        if (flowDirective.getFlowDirectiveEnum() != null) {
          switch (flowDirective.getFlowDirectiveEnum()) {
            case CONTINUE:
              nextTask = iter.hasNext() ? iter.next() : null;
              break;
            case END:
              context.instance().state(WorkflowState.COMPLETED);
            case EXIT:
              nextTask = null;
              break;
          }
        } else {
          nextTask = WorkflowUtils.findTaskByName(iter, flowDirective.getString());
        }
        parentTask.position().back();
      }
    }
    parentTask.position().back();
    parentTask.rawOutput(currentContext.output());
  }

  public static TaskContext<?> executeTask(
      WorkflowContext context, TaskContext<?> parentTask, TaskItem task, JsonNode input) {
    parentTask.position().addProperty(task.getName());
    TaskContext<?> result =
        context
            .definition()
            .taskExecutors()
            .computeIfAbsent(
                parentTask.position().jsonPointer(),
                k ->
                    context
                        .definition()
                        .taskFactory()
                        .getTaskExecutor(task.getTask(), context.definition()))
            .apply(context, parentTask, input);
    parentTask.position().back();
    return result;
  }

  public static WorkflowFilter buildWorkflowFilter(ExpressionFactory exprFactory, String str) {
    assert str != null;
    Expression expression = exprFactory.getExpression(str);
    return expression::eval;
  }

  public static Optional<WorkflowFilter> optionalFilter(ExpressionFactory exprFactory, String str) {
    return str != null ? Optional.of(buildWorkflowFilter(exprFactory, str)) : Optional.empty();
  }
}
