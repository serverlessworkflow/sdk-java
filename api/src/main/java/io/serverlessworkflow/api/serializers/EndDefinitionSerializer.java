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
package io.serverlessworkflow.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.produce.ProduceEvent;
import java.io.IOException;

public class EndDefinitionSerializer extends StdSerializer<End> {

  public EndDefinitionSerializer() {
    this(End.class);
  }

  protected EndDefinitionSerializer(Class<End> t) {
    super(t);
  }

  @Override
  public void serialize(End end, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    if (end != null) {
      if ((end.getProduceEvents() == null || end.getProduceEvents().size() < 1)
          && end.getContinueAs() == null
          && !end.isCompensate()
          && !end.isTerminate()) {
        gen.writeBoolean(true);
      } else {
        gen.writeStartObject();

        if (end.isTerminate()) {
          gen.writeBooleanField("terminate", true);
        }

        if (end.getProduceEvents() != null && !end.getProduceEvents().isEmpty()) {
          gen.writeArrayFieldStart("produceEvents");
          for (ProduceEvent produceEvent : end.getProduceEvents()) {
            gen.writeObject(produceEvent);
          }
          gen.writeEndArray();
        }

        if (end.isCompensate()) {
          gen.writeBooleanField("compensate", true);
        }

        if (end.getContinueAs() != null) {
          gen.writeObjectField("continueAs", end.getContinueAs());
        }

        gen.writeEndObject();
      }
    }
  }
}
