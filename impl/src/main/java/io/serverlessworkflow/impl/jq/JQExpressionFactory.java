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
package io.serverlessworkflow.impl.jq;

import io.serverlessworkflow.impl.Expression;
import io.serverlessworkflow.impl.ExpressionFactory;
import io.serverlessworkflow.impl.ExpressionUtils;
import java.util.function.Supplier;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JQExpressionFactory implements ExpressionFactory {

  private JQExpressionFactory() {}

  private static final JQExpressionFactory instance = new JQExpressionFactory();

  public static JQExpressionFactory get() {
    return instance;
  }

  private static Supplier<Scope> scopeSupplier = new DefaultScopeSupplier();

  private static class DefaultScopeSupplier implements Supplier<Scope> {
    private static class DefaultScope {
      private static Scope scope;

      static {
        scope = Scope.newEmptyScope();
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope);
      }
    }

    @Override
    public Scope get() {
      return DefaultScope.scope;
    }
  }

  @Override
  public Expression getExpression(String expression) {
    try {
      return new JQExpression(scopeSupplier, ExpressionUtils.trimExpr(expression), Versions.JQ_1_6);
    } catch (JsonQueryException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
