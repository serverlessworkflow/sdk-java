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
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.ExportAs;
import io.serverlessworkflow.api.types.ExternalResource;
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.SchemaExternal;
import io.serverlessworkflow.api.types.SchemaInline;
import io.serverlessworkflow.api.types.SchemaUnion;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.fluent.spec.spi.TransformationHandlers;
import java.util.function.Consumer;

public abstract class TaskBaseBuilder<T extends TaskBaseBuilder<T>>
    implements TransformationHandlers {
  private TaskBase task;

  protected TaskBaseBuilder() {}

  protected abstract T self();

  protected final void setTask(TaskBase task) {
    this.task = task;
  }

  public final TaskBase getTask() {
    return task;
  }

  @Override
  public void setInput(Input input) {
    this.task.setInput(input);
  }

  @Override
  public void setExport(Export export) {
    this.task.setExport(export);
  }

  @Override
  public void setOutput(Output output) {
    this.task.setOutput(output);
  }

  /**
   * Conditional to execute this task. Parallel to the `if` conditional in the Spec. Replaced by
   * `when` since `if` is a reserved word.
   *
   * @param expression jq expression to evaluate
   * @see <a
   *     href="https://github.com/serverlessworkflow/specification/blob/main/dsl-reference.md#task">DSL
   *     Reference - Task</a>
   */
  public T when(String expression) {
    this.task.setIf(expression);
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
