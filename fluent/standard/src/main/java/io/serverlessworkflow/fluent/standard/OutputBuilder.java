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
package io.serverlessworkflow.fluent.standard;

import io.serverlessworkflow.api.types.Endpoint;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.OutputAs;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;

public class OutputBuilder {

  private final Output output;

  OutputBuilder() {
    this.output = new Output();
    this.output.setAs(new OutputAs());
    this.output.setSchema(new SchemaUnion());
  }

  public OutputBuilder as(final String expr) {
    this.output.getAs().setString(expr);
    return this;
  }

  public OutputBuilder as(final Object object) {
    this.output.getAs().setObject(object);
    return this;
  }

  public OutputBuilder schema(final String schema) {
    this.output
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

  public OutputBuilder schema(final Object schema) {
    this.output.getSchema().setSchemaInline(new SchemaInline(schema));
    return this;
  }

  public Output build() {
    return this.output;
  }
}
