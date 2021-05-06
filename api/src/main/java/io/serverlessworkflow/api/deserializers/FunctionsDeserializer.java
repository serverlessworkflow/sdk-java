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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.utils.Utils;
import io.serverlessworkflow.api.workflow.Functions;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FunctionsDeserializer extends StdDeserializer<Functions> {

    private static final long serialVersionUID = 510l;
    private static Logger logger = LoggerFactory.getLogger(FunctionsDeserializer.class);

    @SuppressWarnings("unused")
    private WorkflowPropertySource context;

    public FunctionsDeserializer() {
        this(Functions.class);
    }

    public FunctionsDeserializer(Class<?> vc) {
        super(vc);
    }

    public FunctionsDeserializer(WorkflowPropertySource context) {
        this(Functions.class);
        this.context = context;
    }

    @Override
    public Functions deserialize(JsonParser jp,
                                 DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);

        Functions functions = new Functions();
        List<FunctionDefinition> functionDefs = new ArrayList<>();
        if (node.isArray()) {
            for (final JsonNode nodeEle : node) {
                functionDefs.add(mapper.treeToValue(nodeEle, FunctionDefinition.class));
            }
        } else {
            String functionsFileDef = node.asText();
            String functionsFileSrc = Utils.getResourceFileAsString(functionsFileDef);
            JsonNode functionsRefNode;
            ObjectMapper jsonWriter = new ObjectMapper();
            if (functionsFileSrc != null && functionsFileSrc.trim().length() > 0) {
                // if its a yaml def convert to json first
                if (!functionsFileSrc.trim().startsWith("{")) {
                    // convert yaml to json to validate
                    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                    Object obj = yamlReader.readValue(functionsFileSrc, Object.class);

                    functionsRefNode = jsonWriter.readTree(new JSONObject(jsonWriter.writeValueAsString(obj)).toString());
                } else {
                    functionsRefNode = jsonWriter.readTree(new JSONObject(functionsFileSrc).toString());
                }

                JsonNode refFunctions = functionsRefNode.get("functions");
                if (refFunctions != null) {
                    for (final JsonNode nodeEle : refFunctions) {
                        functionDefs.add(mapper.treeToValue(nodeEle, FunctionDefinition.class));
                    }
                } else {
                    logger.error("Unable to find function definitions in reference file: {}", functionsFileSrc);
                }

            } else {
                logger.error("Unable to load function defs reference file: {}", functionsFileSrc);
            }

        }
        functions.setFunctionDefs(functionDefs);
        return functions;

    }
}
