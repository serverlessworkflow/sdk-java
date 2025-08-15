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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class AgenticModelCollection extends JavaModelCollection {

  private final AgenticScope agenticScope;
  private final AgenticScopeCloudEventsHandler ceHandler;

  AgenticModelCollection(AgenticScope agenticScope, AgenticScopeCloudEventsHandler ceHandler) {
    super(Collections.emptyList());
    this.agenticScope = agenticScope;
    this.ceHandler = ceHandler;
  }

  @Override
  public boolean add(WorkflowModel e) {
    Optional<Map<String, Object>> asMap = e.asMap();
    if (asMap.isPresent() && !asMap.get().isEmpty()) {
      this.agenticScope.writeStates(asMap.get());
      return super.add(e);
    }

    // Update the agenticScope with the event body, so agents can use the event data as input
    Object value = e.asJavaObject();
    if (!ceHandler.writeStateIfCloudEvent(this.agenticScope, value)) {
      this.agenticScope.writeState(AgenticModelFactory.DEFAULT_AGENTIC_SCOPE_STATE_KEY, value);
    }

    // add to the collection
    return super.add(e);
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
