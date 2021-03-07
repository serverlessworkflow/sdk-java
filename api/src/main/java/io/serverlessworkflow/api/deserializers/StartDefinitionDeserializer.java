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
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.schedule.Schedule;
import io.serverlessworkflow.api.start.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StartDefinitionDeserializer extends StdDeserializer<Start> {

    private static final long serialVersionUID = 510l;
    private static Logger logger = LoggerFactory.getLogger(StartDefinitionDeserializer.class);

    private WorkflowPropertySource context;

    public StartDefinitionDeserializer() {
        this(Start.class);
    }

    public StartDefinitionDeserializer(Class<?> vc) {
        super(vc);
    }

    public StartDefinitionDeserializer(WorkflowPropertySource context) {
        this(Start.class);
        this.context = context;
    }

    @Override
    public Start deserialize(JsonParser jp,
                              DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);

        Start start = new Start();

        if (!node.isObject()) {
            start.setStateName(node.asText());
            start.setSchedule(null);
            return start;
        } else {
            if(node.get("stateName") != null) {
                start.setStateName(node.get("stateName").asText());
            }

            if(node.get("schedule") != null) {
                start.setSchedule(mapper.treeToValue(node.get("schedule"), Schedule.class));
            }

            return start;

        }

    }
}

