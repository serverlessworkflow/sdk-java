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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;

public class InputBuilder {

  private final Input input;

  InputBuilder() {
    this.input = new Input();
    this.input.setFrom(new InputFrom());
    this.input.setSchema(new SchemaUnion());
  }

  public InputBuilder from(String expr) {
    this.input.getFrom().setString(expr);
    return this;
  }

  public InputBuilder from(Object object) {
    this.input.getFrom().setObject(object);
    return this;
  }

  public InputBuilder schema(Object schema) {
    this.input.getSchema().setSchemaInline(new SchemaInline(schema));
    return this;
  }

  public InputBuilder schema(String schema) {
    this.input
        .getSchema()
        .setSchemaExternal(
            new SchemaExternal()
                .withResource(
                    new ExternalResource()
                        .withEndpoint(
                            new Endpoint()
                                .withUriTemplate(UriTemplateBuilder.newUriTemplate(schema)))));
    return this;
  }

  public Input build() {
    return this.input;
  }
}
