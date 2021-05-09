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
import io.serverlessworkflow.api.schedule.Schedule;

import java.io.IOException;

public class ScheduleSerializer extends StdSerializer<Schedule> {

    public ScheduleSerializer() {
        this(Schedule.class);
    }

    protected ScheduleSerializer(Class<Schedule> t) {
        super(t);
    }

    @Override
    public void serialize(Schedule schedule,
                          JsonGenerator gen,
                          SerializerProvider provider) throws IOException {

        if (schedule != null) {
            if (schedule.getCron() == null
                    && (schedule.getTimezone() == null || schedule.getTimezone().isEmpty())
                    && schedule.getInterval() != null
                    && schedule.getInterval().length() > 0) {
                gen.writeString(schedule.getInterval());
            } else {
                gen.writeStartObject();

                if (schedule.getInterval() != null && schedule.getInterval().length() > 0) {
                    gen.writeStringField("interval", schedule.getInterval());
                }

                if (schedule.getCron() != null) {
                    gen.writeObjectField("cron", schedule.getCron());
                }

                if (schedule.getTimezone() != null && schedule.getTimezone().length() > 0) {
                    gen.writeStringField("timezone", schedule.getTimezone());
                }

                gen.writeEndObject();
            }
        }
    }
}
