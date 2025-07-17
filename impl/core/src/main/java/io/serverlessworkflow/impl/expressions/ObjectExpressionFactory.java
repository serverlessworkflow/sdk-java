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

import io.serverlessworkflow.impl.WorkflowFilter;
import java.util.Map;

public abstract class ObjectExpressionFactory implements ExpressionFactory {

  @Override
  public WorkflowFilter buildFilter(String str, Object object) {
    if (str != null) {
      assert str != null;
      Expression expression = buildExpression(str);
      return expression::eval;
    } else if (object != null) {
      Object exprObj = ExpressionUtils.buildExpressionObject(object, this);
      return exprObj instanceof Map map
          ? (w, t, n) -> modelFactory().from(ExpressionUtils.evaluateExpressionMap(map, w, t, n))
          : (w, t, n) -> modelFactory().fromAny(object);
    }
    throw new IllegalArgumentException("Both object and str are null");
  }
}
