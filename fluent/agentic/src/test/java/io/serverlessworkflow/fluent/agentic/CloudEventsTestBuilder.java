package io.serverlessworkflow.fluent.agentic;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;

public final class CloudEventsTestBuilder {

    private CloudEventsTestBuilder(){

    }

    public static CloudEvent newMessage(String data, String type) {
        if (data == null) {
            data = "";
        }
        return new CloudEventBuilder()
                .withData(data.getBytes())
                .withType(type)
                .withId(UUID.randomUUID().toString())
                .withDataContentType("application/json")
                .withSource(URI.create("test://localhost"))
                .withSubject("A chatbot message")
                .withTime(OffsetDateTime.now())
                .build();
    }

}
