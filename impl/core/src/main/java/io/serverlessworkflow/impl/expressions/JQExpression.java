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
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.util.function.Supplier;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;

public class JQExpression implements Expression {

  private final Supplier<Scope> scope;
  private final String expr;

  private net.thisptr.jackson.jq.Expression internalExpr;

  public JQExpression(Supplier<Scope> scope, String expr, Version version)
      throws JsonQueryException {
    this.expr = expr;
    this.scope = scope;
    this.internalExpr = compile(version);
  }

  private net.thisptr.jackson.jq.Expression compile(Version version) throws JsonQueryException {
    return ExpressionParser.compile(expr, version);
  }

  private interface TypedOutput<T> extends Output {
    T getResult();
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
  public JsonNode eval(WorkflowContext workflow, TaskContext<?> task, JsonNode node) {
    TypedOutput<JsonNode> output = new JsonNodeOutput();
    try {
      internalExpr.apply(createScope(workflow, task), node, output);
      return output.getResult();
    } catch (JsonQueryException e) {
      throw new IllegalArgumentException(
          "Unable to evaluate content " + node + " using expr " + expr, e);
    }
  }

  private Scope createScope(WorkflowContext workflow, TaskContext<?> task) {
    Scope childScope = Scope.newChildScope(scope.get());
    task.variables().forEach((k, v) -> childScope.setValue(k, JsonUtils.fromValue(v)));
    return childScope;
  }
}
