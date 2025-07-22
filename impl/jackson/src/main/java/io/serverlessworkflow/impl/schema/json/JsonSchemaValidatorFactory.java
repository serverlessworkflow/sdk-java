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
package io.serverlessworkflow.impl.schema.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.serverlessworkflow.api.WorkflowFormat;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.resources.StaticResource;
import io.serverlessworkflow.impl.schema.SchemaValidator;
import io.serverlessworkflow.impl.schema.SchemaValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class JsonSchemaValidatorFactory implements SchemaValidatorFactory {

  @Override
  public SchemaValidator getValidator(SchemaInline inline) {
    return new JsonSchemaValidator(JsonUtils.fromValue(inline.getDocument()));
  }

  @Override
  public SchemaValidator getValidator(StaticResource resource) {
    ObjectMapper mapper = WorkflowFormat.fromFileName(resource.name()).mapper();
    try (InputStream in = resource.open()) {
      return new JsonSchemaValidator(mapper.readTree(in));
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    }
  }
}
