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
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.expressions.func.JavaModel;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

class AgenticModel extends JavaModel {

  AgenticModel(AgenticScope agenticScope) {
    super(agenticScope);
  }

  @Override
  public void setObject(Object obj) {
    super.setObject(obj);
  }

  @Override
  public Collection<WorkflowModel> asCollection() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Optional<Map<String, Object>> asMap() {
    return Optional.of(((AgenticScope) object).state());
  }

  @Override
  public <T> Optional<T> as(Class<T> clazz) {
    if (AgenticScope.class.isAssignableFrom(clazz)) {
      return Optional.of(clazz.cast(object));
    } else if (Map.class.isAssignableFrom(clazz)) {
      return Optional.of(clazz.cast(((AgenticScope) object).state()));
    } else {
      return super.as(clazz);
    }
  }
}
