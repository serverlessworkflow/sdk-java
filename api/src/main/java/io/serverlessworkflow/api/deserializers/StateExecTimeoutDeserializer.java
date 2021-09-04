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
package io.serverlessworkflow.api.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.timeouts.StateExecTimeout;

import java.io.IOException;

public class StateExecTimeoutDeserializer extends StdDeserializer<StateExecTimeout> {

    private static final long serialVersionUID = 510l;

    @SuppressWarnings("unused")
    private WorkflowPropertySource context;

    public StateExecTimeoutDeserializer() {
        this(StateExecTimeout.class);
    }

    public StateExecTimeoutDeserializer(Class<?> vc) {
        super(vc);
    }

    public StateExecTimeoutDeserializer(WorkflowPropertySource context) {
        this(StateExecTimeout.class);
        this.context = context;
    }

    @Override
    public StateExecTimeout deserialize(JsonParser jp,
                                        DeserializationContext ctxt) throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        StateExecTimeout stateExecTimeout = new StateExecTimeout();

        if (!node.isObject()) {
            stateExecTimeout.setTotal(node.asText());
            stateExecTimeout.setSingle(null);
            return stateExecTimeout;
        } else {
            if (node.get("single") != null) {
                stateExecTimeout.setSingle(node.get("single").asText());
            }

            if (node.get("total") != null) {
                stateExecTimeout.setTotal(node.get("total").asText());
            }

            return stateExecTimeout;
        }
    }
}
