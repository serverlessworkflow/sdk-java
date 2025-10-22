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
package io.serverlessworkflow.impl.model.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.model.func.JavaModel;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

class AgenticModel extends JavaModel {

  private final AgenticScope agenticScope;

  AgenticModel(AgenticScope agenticScope, Object object) {
    super(object);
    this.agenticScope = agenticScope;
  }

  public AgenticScope getAgenticScope() {
    return agenticScope;
  }

  @Override
  public Collection<WorkflowModel> asCollection() {
    throw new UnsupportedOperationException("asCollection() is not supported yet.");
  }

  @Override
  public Optional<Map<String, Object>> asMap() {
    return Optional.of(this.agenticScope.state());
  }

  @Override
  public Object asJavaObject() {
    return agenticScope;
  }

  @Override
  protected <T> Optional<T> convert(Class<T> clazz) {
    return AgenticScope.class.isAssignableFrom(clazz)
        ? Optional.of(clazz.cast(this.agenticScope))
        : super.convert(clazz);
  }
}
