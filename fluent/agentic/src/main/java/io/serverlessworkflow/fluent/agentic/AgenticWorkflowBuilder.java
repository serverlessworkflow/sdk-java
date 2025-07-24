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
package io.serverlessworkflow.fluent.agentic;

import static java.lang.constant.ConstantDescs.DEFAULT_NAME;

import io.serverlessworkflow.fluent.spec.BaseWorkflowBuilder;

public final class AgenticWorkflowBuilder
    extends BaseWorkflowBuilder<
        AgenticWorkflowBuilder, AgentDoTaskBuilder, AgentTaskItemListBuilder> {

  AgenticWorkflowBuilder(final String name, final String namespace, final String version) {
    super(name, namespace, version);
  }

  public static AgenticWorkflowBuilder workflow() {
    return new AgenticWorkflowBuilder(DEFAULT_NAME, DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static AgenticWorkflowBuilder workflow(String name) {
    return new AgenticWorkflowBuilder(name, DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static AgenticWorkflowBuilder workflow(String name, String ns) {
    return new AgenticWorkflowBuilder(name, ns, DEFAULT_VERSION);
  }

  @Override
  protected AgentDoTaskBuilder newDo() {
    return new AgentDoTaskBuilder();
  }

  @Override
  protected AgenticWorkflowBuilder self() {
    return this;
  }
}
