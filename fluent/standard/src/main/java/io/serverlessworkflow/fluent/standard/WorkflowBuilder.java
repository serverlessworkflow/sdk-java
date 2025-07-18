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
import io.serverlessworkflow.api.types.Workflow;
import java.util.UUID;
import java.util.function.Consumer;

public class WorkflowBuilder {

  private static final String DSL = "1.0.0";
  private static final String DEFAULT_VERSION = "0.0.1";
  private static final String DEFAULT_NAMESPACE = "org.acme";

  private final Workflow workflow;
  private final Document document;

  private WorkflowBuilder(final String name, final String namespace, final String version) {
    this.document = new Document();
    this.document.setName(name);
    this.document.setNamespace(namespace);
    this.document.setVersion(version);
    this.document.setDsl(DSL);
    this.workflow = new Workflow();
    this.workflow.setDocument(this.document);
  }

  public static WorkflowBuilder workflow(
      final String name, final String namespace, final String version) {
    return new WorkflowBuilder(name, namespace, version);
  }

  public static WorkflowBuilder workflow(final String name, final String namespace) {
    return new WorkflowBuilder(name, namespace, DEFAULT_VERSION);
  }

  public static WorkflowBuilder workflow(final String name) {
    return new WorkflowBuilder(name, DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static WorkflowBuilder workflow() {
    return new WorkflowBuilder(UUID.randomUUID().toString(), DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public WorkflowBuilder document(Consumer<DocumentBuilder> documentBuilderConsumer) {
    final DocumentBuilder documentBuilder = new DocumentBuilder(this.document);
    documentBuilderConsumer.accept(documentBuilder);
    return this;
  }

  public WorkflowBuilder use(Consumer<UseBuilder> useBuilderConsumer) {
    final UseBuilder builder = new UseBuilder();
    useBuilderConsumer.accept(builder);
    this.workflow.setUse(builder.build());
    return this;
  }

  public WorkflowBuilder doTasks(Consumer<DoTaskBuilder> doTaskConsumer) {
    final DoTaskBuilder doTaskBuilder = new DoTaskBuilder();
    doTaskConsumer.accept(doTaskBuilder);
    this.workflow.setDo(doTaskBuilder.build().getDo());
    return this;
  }

  public WorkflowBuilder input(Consumer<InputBuilder> inputBuilderConsumer) {
    final InputBuilder inputBuilder = new InputBuilder();
    inputBuilderConsumer.accept(inputBuilder);
    this.workflow.setInput(inputBuilder.build());
    return this;
  }

  public WorkflowBuilder output(Consumer<OutputBuilder> outputBuilderConsumer) {
    final OutputBuilder outputBuilder = new OutputBuilder();
    outputBuilderConsumer.accept(outputBuilder);
    this.workflow.setOutput(outputBuilder.build());
    return this;
  }

  public Workflow build() {
    return this.workflow;
  }
}
