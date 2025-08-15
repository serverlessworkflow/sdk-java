package io.serverlessworkflow.impl.expressions.agentic;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;

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
                return mapper.readValue(ce.toBytes(), new TypeReference<>() {
                });
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
