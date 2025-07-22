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
package io.serverlessworkflow.impl.expressions.jq;

import static io.serverlessworkflow.impl.jackson.JsonUtils.modelToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.expressions.Expression;
import io.serverlessworkflow.impl.expressions.TaskDescriptor;
import io.serverlessworkflow.impl.expressions.WorkflowDescriptor;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.util.function.Supplier;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JQExpression implements Expression {

  private final Supplier<Scope> scope;
  private final String expr;
  private final net.thisptr.jackson.jq.Expression internalExpr;
  private final WorkflowModelFactory modelFactory;

  public JQExpression(
      Supplier<Scope> scope, String expr, Version version, WorkflowModelFactory modelFactory)
      throws JsonQueryException {
    this.expr = expr;
    this.scope = scope;
    this.internalExpr = ExpressionParser.compile(expr, version);
    this.modelFactory = modelFactory;
  }

  @Override
  public WorkflowModel eval(WorkflowContext workflow, TaskContext task, WorkflowModel model) {
    JsonNodeOutput output = new JsonNodeOutput();
    JsonNode node = modelToJson(model);
    try {
      internalExpr.apply(createScope(workflow, task), node, output);
      return modelFactory.fromAny(output.getResult());
    } catch (JsonQueryException e) {
      throw new IllegalArgumentException(
          "Unable to evaluate content " + node + " using expr " + expr, e);
    }
  }

  private static class JsonNodeOutput implements Output {
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

    public JsonNode getResult() {
      return result;
    }
  }

  private Scope createScope(WorkflowContext workflow, TaskContext task) {
    Scope childScope = Scope.newChildScope(scope.get());
    if (task != null) {
      childScope.setValue("input", modelToJson(task.input()));
      childScope.setValue("output", modelToJson(task.output()));
      childScope.setValue("task", () -> JsonUtils.fromValue(TaskDescriptor.of(task)));
      task.variables().forEach((k, v) -> childScope.setValue(k, JsonUtils.fromValue(v)));
    }
    if (workflow != null) {
      childScope.setValue("context", modelToJson(workflow.context()));
      childScope.setValue(
          "runtime",
          () ->
              JsonUtils.fromValue(
                  workflow.definition().application().runtimeDescriptorFactory().get()));
      childScope.setValue("workflow", () -> JsonUtils.fromValue(WorkflowDescriptor.of(workflow)));
    }
    return childScope;
  }
}
