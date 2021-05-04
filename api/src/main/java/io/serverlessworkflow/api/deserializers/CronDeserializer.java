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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.cron.Cron;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;

import java.io.IOException;

public class CronDeserializer extends StdDeserializer<Cron> {

    private static final long serialVersionUID = 510l;

    private WorkflowPropertySource context;

    public CronDeserializer() {
        this(Cron.class);
    }

    public CronDeserializer(Class<?> vc) {
        super(vc);
    }

    public CronDeserializer(WorkflowPropertySource context) {
        this(Cron.class);
        this.context = context;
    }

    @Override
    public Cron deserialize(JsonParser jp,
                                   DeserializationContext ctxt) throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        Cron cron = new Cron();

        if (!node.isObject()) {
            cron.setExpression(node.asText());
            return cron;
        } else {
            if(node.get("expression") != null) {
                cron.setExpression(node.get("expression").asText());
            }

            if(node.get("validUntil") != null) {
                cron.setValidUntil(node.get("validUntil").asText());
            }

            return cron;
        }
    }
}

