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
package io.serverlessworkflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import io.serverlessworkflow.api.types.Workflow;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.stream.Collectors;

class ValidationReader implements WorkflowReaderOperations {
  private final JsonSchema schemaObject;

  ValidationReader() {
    try (InputStream input =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("schema/workflow.yaml")) {
      this.schemaObject =
          JsonSchemaFactory.getInstance(VersionFlag.V7)
              .getSchema(input, InputFormat.YAML, SchemaValidatorsConfig.builder().build());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Workflow read(InputStream input, WorkflowFormat format) throws IOException {
    return validate(format.mapper().readValue(input, JsonNode.class), format);
  }

  @Override
  public Workflow read(Reader input, WorkflowFormat format) throws IOException {
    return validate(format.mapper().readValue(input, JsonNode.class), format);
  }

  @Override
  public Workflow read(byte[] input, WorkflowFormat format) throws IOException {
    return validate(format.mapper().readValue(input, JsonNode.class), format);
  }

  @Override
  public Workflow read(String input, WorkflowFormat format) throws IOException {
    return validate(format.mapper().readValue(input, JsonNode.class), format);
  }

  private Workflow validate(JsonNode value, WorkflowFormat format) {
    Set<ValidationMessage> validationErrors = schemaObject.validate(value);
    if (!validationErrors.isEmpty()) {
      throw new IllegalArgumentException(
          validationErrors.stream()
              .map(ValidationMessage::toString)
              .collect(Collectors.joining("\n")));
    }
    return format.mapper().convertValue(value, Workflow.class);
  }
}
