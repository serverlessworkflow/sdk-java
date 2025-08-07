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
package io.serverlessworkflow.impl.expressions.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.expressions.func.JavaModelCollection;
import java.util.Collection;
import java.util.Optional;

class AgenticModelCollection extends JavaModelCollection {

  private final AgenticScope agenticScope;

  AgenticModelCollection(Collection<?> object, AgenticScope agenticScope) {
    super(object);
    this.agenticScope = agenticScope;
  }

  AgenticModelCollection(AgenticScope agenticScope) {
    this.agenticScope = agenticScope;
  }

  @Override
  protected WorkflowModel nextItem(Object obj) {
    return new AgenticModel((AgenticScope) obj);
  }

  @Override
  public <T> Optional<T> as(Class<T> clazz) {
    if (AgenticScope.class.isAssignableFrom(clazz)) {
      return Optional.of(clazz.cast(agenticScope));
    } else if (ResultWithAgenticScope.class.isAssignableFrom(clazz)) {
      return Optional.of(clazz.cast(new ResultWithAgenticScope<>(agenticScope, object)));
    } else {
      return super.as(clazz);
    }
  }
}
