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
package io.serverlessworkflow.fluent.agentic;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CloudEventsTestBuilder {

  private CloudEventsTestBuilder() {}

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
