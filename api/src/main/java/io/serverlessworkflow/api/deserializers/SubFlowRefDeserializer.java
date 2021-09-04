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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.functions.SubFlowRef;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;

import java.io.IOException;

public class SubFlowRefDeserializer extends StdDeserializer<SubFlowRef> {

    private static final long serialVersionUID = 510l;

    @SuppressWarnings("unused")
    private WorkflowPropertySource context;

    public SubFlowRefDeserializer() {
        this(SubFlowRef.class);
    }

    public SubFlowRefDeserializer(Class<?> vc) {
        super(vc);
    }

    public SubFlowRefDeserializer(WorkflowPropertySource context) {
        this(SubFlowRef.class);
        this.context = context;
    }

    @Override
    public SubFlowRef deserialize(JsonParser jp,
                                  DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);

        SubFlowRef subflowRef = new SubFlowRef();

        if (!node.isObject()) {
            subflowRef.setWorkflowId(node.asText());
            return subflowRef;
        } else {
            if (node.get("workflowId") != null) {
                subflowRef.setWorkflowId(node.get("workflowId").asText());
            }

            if (node.get("version") != null) {
                subflowRef.setVersion(node.get("version").asText());
            }

            return subflowRef;
        }
    }
}

