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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.expressions.func.JavaModelCollection;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AgenticModelCollection extends JavaModelCollection {

  private final AgenticScope agenticScope;
  private final ObjectMapper mapper = new ObjectMapper();

  AgenticModelCollection(AgenticScope agenticScope) {
    super(Collections.emptyList());
    this.agenticScope = agenticScope;
  }

  @Override
  public boolean add(WorkflowModel e) {
    Optional<Map<String, Object>> asMap = e.asMap();
    if (asMap.isPresent()) {
      this.agenticScope.writeStates(asMap.get());
    } else {
      // Update the agenticScope with the event body, so agents can use the event data as input
      Object javaObj = e.asJavaObject();
      if (javaObj instanceof CloudEvent) {
        try {
          this.agenticScope.writeStates(
              mapper.readValue(
                  Objects.requireNonNull(((CloudEvent) javaObj).getData()).toString(),
                  new TypeReference<>() {}));
        } catch (JsonProcessingException ex) {
          throw new IllegalArgumentException(
              "Unable to parse CloudEvent, data must be a valid JSON", ex);
        }
      } else if (javaObj instanceof CloudEventData) {
        try {
          this.agenticScope.writeStates(
              mapper.readValue(
                  Objects.requireNonNull(((CloudEventData) javaObj)).toBytes(),
                  new TypeReference<>() {}));
        } catch (IOException ex) {
          throw new IllegalArgumentException(
              "Unable to parse CloudEventData, data must be a valid JSON", ex);
        }
      } else {
        this.agenticScope.writeState(AgenticModelFactory.DEFAULT_AGENTIC_SCOPE_STATE_KEY, javaObj);
      }
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
