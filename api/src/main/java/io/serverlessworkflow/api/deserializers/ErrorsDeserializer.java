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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.serverlessworkflow.api.error.ErrorDefinition;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.utils.Utils;
import io.serverlessworkflow.api.workflow.Errors;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ErrorsDeserializer extends StdDeserializer<Errors> {

    private static final long serialVersionUID = 510l;
    private static Logger logger = LoggerFactory.getLogger(ErrorsDeserializer.class);

    @SuppressWarnings("unused")
    private WorkflowPropertySource context;

    public ErrorsDeserializer() {
        this(Errors.class);
    }

    public ErrorsDeserializer(Class<?> vc) {
        super(vc);
    }

    public ErrorsDeserializer(WorkflowPropertySource context) {
        this(Errors.class);
        this.context = context;
    }

    @Override
    public Errors deserialize(JsonParser jp,
                               DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = jp.getCodec().readTree(jp);

        Errors errors = new Errors();
        List<ErrorDefinition> errorDefinitions = new ArrayList<>();

        if (node.isArray()) {
            for (final JsonNode nodeEle : node) {
                errorDefinitions.add(mapper.treeToValue(nodeEle, ErrorDefinition.class));
            }
        } else {
            String errorsFileDef = node.asText();
            String errorsFileSrc = Utils.getResourceFileAsString(errorsFileDef);
            JsonNode errorsRefNode;
            ObjectMapper jsonWriter = new ObjectMapper();
            if (errorsFileSrc != null && errorsFileSrc.trim().length() > 0) {
                // if its a yaml def convert to json first
                if (!errorsFileSrc.trim().startsWith("{")) {
                    // convert yaml to json to validate
                    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                    Object obj = yamlReader.readValue(errorsFileSrc, Object.class);

                    errorsRefNode = jsonWriter.readTree(new JSONObject(jsonWriter.writeValueAsString(obj)).toString());
                } else {
                    errorsRefNode = jsonWriter.readTree(new JSONObject(errorsFileSrc).toString());
                }

                JsonNode refErrors = errorsRefNode.get("errors");
                if (refErrors != null) {
                    for (final JsonNode nodeEle : refErrors) {
                        errorDefinitions.add(mapper.treeToValue(nodeEle, ErrorDefinition.class));
                    }
                } else {
                    logger.error("Unable to find error definitions in reference file: {}", errorsFileSrc);
                }

            } else {
                logger.error("Unable to load errors defs reference file: {}", errorsFileSrc);
            }

        }
        errors.setErrorDefs(errorDefinitions);
        return errors;

    }
}

