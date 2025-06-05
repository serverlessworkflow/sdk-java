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

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.Extension;
import io.serverlessworkflow.api.interfaces.State;

public class WorkflowSerializer extends StdSerializer<Workflow> {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public WorkflowSerializer() {
        this(Workflow.class);
    }

    protected WorkflowSerializer(Class<Workflow> t) {
        super(t);
    }

    protected static String generateUniqueId() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes("UTF-8"));
            return bytesToHex(salt.digest());
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Helper to write either an array of items or a single string reference
    private <T> void writeListOrRef(
            JsonGenerator gen,
            SerializerProvider prov,
            String fieldName,
            List<T> items,
            String refValue) throws IOException {
        if (items != null && !items.isEmpty()) {
            gen.writeArrayFieldStart(fieldName);
            for (T item : items) {
                prov.defaultSerializeValue(item, gen);
            }
            gen.writeEndArray();
        } else if (refValue != null) {
            gen.writeStringField(fieldName, refValue);
        }
    }

    @Override
    public void serialize(Workflow workflow, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        gen.writeStartObject();

        // --- ID / key / basic fields ---
        if (workflow.getId() != null && !workflow.getId().isEmpty()) {
            gen.writeStringField("id", workflow.getId());
        } else {
            gen.writeStringField("id", generateUniqueId());
        }
        if (workflow.getKey() != null) {
            gen.writeStringField("key", workflow.getKey());
        }
        gen.writeStringField("name", workflow.getName());
        if (workflow.getDescription() != null && !workflow.getDescription().isEmpty()) {
            gen.writeStringField("description", workflow.getDescription());
        }
        if (workflow.getVersion() != null && !workflow.getVersion().isEmpty()) {
            gen.writeStringField("version", workflow.getVersion());
        }
        if (workflow.getAnnotations() != null && !workflow.getAnnotations().isEmpty()) {
            gen.writeObjectField("annotations", workflow.getAnnotations());
        }
        if (workflow.getDataInputSchema() != null) {
            if (workflow.getDataInputSchema().getSchema() != null
                    && !workflow.getDataInputSchema().getSchema().isEmpty()
                    && workflow.getDataInputSchema().isFailOnValidationErrors()) {
                gen.writeStringField("dataInputSchema", workflow.getDataInputSchema().getSchema());
            } else if (workflow.getDataInputSchema().getSchema() != null
                    && !workflow.getDataInputSchema().getSchema().isEmpty()
                    && !workflow.getDataInputSchema().isFailOnValidationErrors()) {
                gen.writeObjectField("dataInputSchema", workflow.getDataInputSchema());
            }
        }
        if (workflow.getStart() != null) {
            gen.writeObjectField("start", workflow.getStart());
        }
        if (workflow.getSpecVersion() != null && !workflow.getSpecVersion().isEmpty()) {
            gen.writeStringField("specVersion", workflow.getSpecVersion());
        }
        if (workflow.getExpressionLang() != null && !workflow.getExpressionLang().isEmpty()) {
            gen.writeStringField("expressionLang", workflow.getExpressionLang());
        }
        if (workflow.isKeepActive()) {
            gen.writeBooleanField("keepActive", workflow.isKeepActive());
        }
        if (workflow.isAutoRetries()) {
            gen.writeBooleanField("autoRetries", workflow.isAutoRetries());
        }
        if (workflow.getMetadata() != null && !workflow.getMetadata().isEmpty()) {
            gen.writeObjectField("metadata", workflow.getMetadata());
        }

        // --- Collections or references ---
        if (workflow.getEvents() != null) {
            writeListOrRef(gen, provider,
                    "events",
                    workflow.getEvents().getEventDefs(),
                    workflow.getEvents().getRefValue());
        }
        if (workflow.getFunctions() != null) {
            writeListOrRef(gen, provider,
                    "functions",
                    workflow.getFunctions().getFunctionDefs(),
                    workflow.getFunctions().getRefValue());
        }
        if (workflow.getRetries() != null) {
            writeListOrRef(gen, provider,
                    "retries",
                    workflow.getRetries().getRetryDefs(),
                    workflow.getRetries().getRefValue());
        }
        if (workflow.getErrors() != null) {
            writeListOrRef(gen, provider,
                    "errors",
                    workflow.getErrors().getErrorDefs(),
                    workflow.getErrors().getRefValue());
        }
        if (workflow.getSecrets() != null) {
            writeListOrRef(gen, provider,
                    "secrets",
                    workflow.getSecrets().getSecretDefs(),
                    workflow.getSecrets().getRefValue());
        }

        // --- Always-array fields ---
        if (workflow.getStates() != null && !workflow.getStates().isEmpty()) {
            gen.writeArrayFieldStart("states");
            for (State state : workflow.getStates()) {
                gen.writeObject(state);
            }
            gen.writeEndArray();
        }
        if (workflow.getExtensions() != null && !workflow.getExtensions().isEmpty()) {
            gen.writeArrayFieldStart("extensions");
            for (Extension ext : workflow.getExtensions()) {
                gen.writeObject(ext);
            }
            gen.writeEndArray();
        }

        gen.writeEndObject();
    }
}
