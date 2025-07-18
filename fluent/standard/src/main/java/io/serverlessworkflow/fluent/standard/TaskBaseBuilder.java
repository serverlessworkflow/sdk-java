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
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.ExportAs;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;
import io.serverlessworkflow.api.types.TaskBase;
import java.util.function.Consumer;

public abstract class TaskBaseBuilder<T extends TaskBaseBuilder<T>> {
  protected abstract T self();

  private TaskBase task;

  protected TaskBaseBuilder() {}

  protected void setTask(TaskBase task) {
    this.task = task;
  }

  public T _if(String id) {
    this.task.setIf(id);
    return self();
  }

  public T then(FlowDirectiveEnum then) {
    this.task.setThen(new FlowDirective().withFlowDirectiveEnum(then));
    return self();
  }

  public T exportAs(Object exportAs) {
    this.task.setExport(new ExportBuilder().as(exportAs).build());
    return self();
  }

  public T export(Consumer<ExportBuilder> exportConsumer) {
    final ExportBuilder exportBuilder = new ExportBuilder();
    exportConsumer.accept(exportBuilder);
    this.task.setExport(exportBuilder.build());
    return self();
  }

  public T input(Consumer<InputBuilder> inputConsumer) {
    final InputBuilder inputBuilder = new InputBuilder();
    inputConsumer.accept(inputBuilder);
    this.task.setInput(inputBuilder.build());
    return self();
  }

  public T output(Consumer<OutputBuilder> outputConsumer) {
    final OutputBuilder outputBuilder = new OutputBuilder();
    outputConsumer.accept(outputBuilder);
    this.task.setOutput(outputBuilder.build());
    return self();
  }

  // TODO: add timeout, metadata

  public static final class ExportBuilder {
    private final Export export;

    public ExportBuilder() {
      this.export = new Export();
      this.export.setAs(new ExportAs());
      this.export.setSchema(new SchemaUnion());
    }

    public ExportBuilder as(Object as) {
      this.export.getAs().withObject(as);
      return this;
    }

    public ExportBuilder as(String as) {
      this.export.getAs().withString(as);
      return this;
    }

    public ExportBuilder schema(String schema) {
      this.export
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

    public ExportBuilder schema(Object schema) {
      this.export.getSchema().setSchemaInline(new SchemaInline(schema));
      return this;
    }

    public Export build() {
      return this.export;
    }
  }
}
