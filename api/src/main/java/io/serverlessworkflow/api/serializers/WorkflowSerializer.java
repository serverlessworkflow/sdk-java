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
import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.error.ErrorDefinition;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.interfaces.Extension;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.retry.RetryDefinition;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

public class WorkflowSerializer extends StdSerializer<Workflow> {

  public WorkflowSerializer() {
    this(Workflow.class);
  }

  protected WorkflowSerializer(Class<Workflow> t) {
    super(t);
  }

  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  @Override
  public void serialize(Workflow workflow, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    gen.writeStartObject();

    if (workflow.getId() != null && !workflow.getId().isEmpty()) {
      gen.writeStringField("id", workflow.getId());
    } else {
      gen.writeStringField("id", generateUniqueId());
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
          && workflow.getDataInputSchema().getSchema().length() > 0
          && workflow.getDataInputSchema().isFailOnValidationErrors()) {
        gen.writeStringField("dataInputSchema", workflow.getDataInputSchema().getSchema());

      } else if (workflow.getDataInputSchema().getSchema() != null
          && workflow.getDataInputSchema().getSchema().length() > 0
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

    if (workflow.getExtensions() != null && !workflow.getExpressionLang().isEmpty()) {
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

    if (workflow.getEvents() != null && !workflow.getEvents().getEventDefs().isEmpty()) {
      gen.writeArrayFieldStart("events");
      for (EventDefinition eventDefinition : workflow.getEvents().getEventDefs()) {
        gen.writeObject(eventDefinition);
      }
      gen.writeEndArray();
    }

    if (workflow.getFunctions() != null && !workflow.getFunctions().getFunctionDefs().isEmpty()) {
      gen.writeArrayFieldStart("functions");
      for (FunctionDefinition function : workflow.getFunctions().getFunctionDefs()) {
        gen.writeObject(function);
      }
      gen.writeEndArray();
    }

    if (workflow.getRetries() != null && !workflow.getRetries().getRetryDefs().isEmpty()) {
      gen.writeArrayFieldStart("retries");
      for (RetryDefinition retry : workflow.getRetries().getRetryDefs()) {
        gen.writeObject(retry);
      }
      gen.writeEndArray();
    }

    if (workflow.getErrors() != null && !workflow.getErrors().getErrorDefs().isEmpty()) {
      gen.writeArrayFieldStart("errors");
      for (ErrorDefinition error : workflow.getErrors().getErrorDefs()) {
        gen.writeObject(error);
      }
      gen.writeEndArray();
    }

    if (workflow.getSecrets() != null && !workflow.getSecrets().getSecretDefs().isEmpty()) {
      gen.writeArrayFieldStart("secrets");
      for (String secretDef : workflow.getSecrets().getSecretDefs()) {
        gen.writeString(secretDef);
      }
      gen.writeEndArray();
    }

    if (workflow.getConstants() != null && !workflow.getConstants().getConstantsDef().isEmpty()) {
      gen.writeObjectField("constants", workflow.getConstants().getConstantsDef());
    }

    if (workflow.getTimeouts() != null) {
      gen.writeObjectField("timeouts", workflow.getTimeouts());
    }

    if (workflow.getAuth() != null && !workflow.getAuth().getAuthDefs().isEmpty()) {
      gen.writeObjectField("auth", workflow.getAuth().getAuthDefs());
    }

    if (workflow.getStates() != null && !workflow.getStates().isEmpty()) {
      gen.writeArrayFieldStart("states");
      for (State state : workflow.getStates()) {
        gen.writeObject(state);
      }
      gen.writeEndArray();
    }

    if (workflow.getExtensions() != null && !workflow.getExtensions().isEmpty()) {
      gen.writeArrayFieldStart("extensions");
      for (Extension extension : workflow.getExtensions()) {
        gen.writeObject(extension);
      }
      gen.writeEndArray();
    }

    gen.writeEndObject();
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
}
