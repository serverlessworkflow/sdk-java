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

import io.serverlessworkflow.fluent.func.spi.FuncTransformations;
import io.serverlessworkflow.fluent.spec.BaseWorkflowBuilder;
import java.util.UUID;

public class AgentWorkflowBuilder
    extends BaseWorkflowBuilder<AgentWorkflowBuilder, AgentDoTaskBuilder, AgentTaskItemListBuilder>
    implements FuncTransformations<AgentWorkflowBuilder> {

  AgentWorkflowBuilder(final String name, final String namespace, final String version) {
    super(name, namespace, version);
  }

  public static AgentWorkflowBuilder workflow() {
    return new AgentWorkflowBuilder(
        UUID.randomUUID().toString(), DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static AgentWorkflowBuilder workflow(String name) {
    return new AgentWorkflowBuilder(name, DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static AgentWorkflowBuilder workflow(String name, String ns) {
    return new AgentWorkflowBuilder(name, ns, DEFAULT_VERSION);
  }

  @Override
  protected AgentDoTaskBuilder newDo() {
    return new AgentDoTaskBuilder();
  }

  @Override
  protected AgentWorkflowBuilder self() {
    return this;
  }
}
