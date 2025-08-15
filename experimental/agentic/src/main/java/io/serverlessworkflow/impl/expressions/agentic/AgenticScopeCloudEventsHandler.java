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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import java.io.IOException;
import java.util.Map;

public final class AgenticScopeCloudEventsHandler {

  private final ObjectMapper mapper = new ObjectMapper();

  AgenticScopeCloudEventsHandler() {}

  public void writeState(final AgenticScope scope, final CloudEvent cloudEvent) {
    if (cloudEvent != null) {
      writeState(scope, cloudEvent.getData());
    }
  }

  public void writeState(final AgenticScope scope, final CloudEventData cloudEvent) {
    scope.writeStates(extractDataAsMap(cloudEvent));
  }

  public boolean writeStateIfCloudEvent(final AgenticScope scope, final Object value) {
    if (value instanceof CloudEvent) {
      writeState(scope, (CloudEvent) value);
      return true;
    } else if (value instanceof CloudEventData) {
      writeState(scope, (CloudEventData) value);
      return true;
    }
    return false;
  }

  public Map<String, Object> extractDataAsMap(final CloudEventData ce) {
    try {
      if (ce != null) {
        return mapper.readValue(ce.toBytes(), new TypeReference<>() {});
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to parse CloudEvent data as JSON", e);
    }
    return Map.of();
  }

  public Map<String, Object> extractDataAsMap(final CloudEvent ce) {
    if (ce != null) {
      return extractDataAsMap(ce.getData());
    }
    return Map.of();
  }
}
