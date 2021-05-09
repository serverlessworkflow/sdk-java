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
import io.serverlessworkflow.api.start.Start;

import java.io.IOException;

public class StartDefinitionSerializer extends StdSerializer<Start> {

    public StartDefinitionSerializer() {
        this(Start.class);
    }

    protected StartDefinitionSerializer(Class<Start> t) {
        super(t);
    }

    @Override
    public void serialize(Start start,
                          JsonGenerator gen,
                          SerializerProvider provider) throws IOException {

        if (start != null) {
            if (start.getStateName() != null && start.getStateName().length() > 0
                    && start.getSchedule() == null) {
                gen.writeString(start.getStateName());
            } else {
                gen.writeStartObject();

                if (start.getStateName() != null && start.getStateName().length() > 0) {
                    gen.writeStringField("stateName", start.getStateName());
                }

                if (start.getSchedule() != null) {
                    gen.writeObjectField("schedule",
                            start.getSchedule());
                }

                gen.writeEndObject();
            }
        }
    }
}
