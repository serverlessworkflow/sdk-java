/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.serverlessworkflow.api.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.cron.Cron;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.schedule.Schedule;

import java.io.IOException;

public class ScheduleDeserializer extends StdDeserializer<Schedule> {

    private static final long serialVersionUID = 510l;

    @SuppressWarnings("unused")
    private WorkflowPropertySource context;

    public ScheduleDeserializer() {
        this(Schedule.class);
    }

    public ScheduleDeserializer(Class<?> vc) {
        super(vc);
    }

    public ScheduleDeserializer(WorkflowPropertySource context) {
        this(Schedule.class);
        this.context = context;
    }

    @Override
    public Schedule deserialize(JsonParser jp,
                            DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);

        Schedule schedule = new Schedule();

        if (!node.isObject()) {
            schedule.setInterval(node.asText());
            return schedule;
        } else {
            if(node.get("interval") != null) {
                schedule.setInterval(node.get("interval").asText());
            }

            if(node.get("cron") != null) {
                schedule.setCron(mapper.treeToValue(node.get("cron"), Cron.class));
            }

            if(node.get("timezone") != null) {
                schedule.setTimezone(node.get("timezone").asText());
            }

            return schedule;
        }
    }
}