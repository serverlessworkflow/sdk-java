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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Export;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.Output;
import io.serverlessworkflow.api.types.Workflow;
import java.util.function.Consumer;

public abstract class BaseWorkflowBuilder<
        W extends BaseWorkflowBuilder<W, DB>, DB extends BaseDoTaskBuilder<DB>>
    implements TransformationHandlers {

  public static final String DSL = "1.0.0";
  public static final String DEFAULT_VERSION = "0.0.1";
  public static final String DEFAULT_NAMESPACE = "org.acme";

  private final Workflow workflow;
  private final Document document;

  protected BaseWorkflowBuilder(final String name, final String namespace, final String version) {
    this.document = new Document();
    this.document.setName(name);
    this.document.setNamespace(namespace);
    this.document.setVersion(version);
    this.document.setDsl(DSL);
    this.workflow = new Workflow();
    this.workflow.setDocument(this.document);
  }

  protected abstract DB newDo();

  protected abstract W self();

  @Override
  public void setOutput(Output output) {
    this.workflow.setOutput(output);
  }

  @Override
  public void setExport(Export export) {
    // TODO: build another interface with only Output and Input
  }

  @Override
  public void setInput(Input input) {
    this.workflow.setInput(input);
  }

  public W document(Consumer<DocumentBuilder> documentBuilderConsumer) {
    final DocumentBuilder documentBuilder = new DocumentBuilder(this.document);
    documentBuilderConsumer.accept(documentBuilder);
    return self();
  }

  public W use(Consumer<UseBuilder> useBuilderConsumer) {
    final UseBuilder builder = new UseBuilder();
    useBuilderConsumer.accept(builder);
    this.workflow.setUse(builder.build());
    return self();
  }

  public W doTasks(Consumer<DB> doTaskConsumer) {
    final DB doTaskBuilder = newDo();
    doTaskConsumer.accept(doTaskBuilder);
    this.workflow.setDo(doTaskBuilder.build().getDo());
    return self();
  }

  public W input(Consumer<InputBuilder> inputBuilderConsumer) {
    final InputBuilder inputBuilder = new InputBuilder();
    inputBuilderConsumer.accept(inputBuilder);
    this.workflow.setInput(inputBuilder.build());
    return self();
  }

  public W output(Consumer<OutputBuilder> outputBuilderConsumer) {
    final OutputBuilder outputBuilder = new OutputBuilder();
    outputBuilderConsumer.accept(outputBuilder);
    this.workflow.setOutput(outputBuilder.build());
    return self();
  }

  public Workflow build() {
    return this.workflow;
  }
}
