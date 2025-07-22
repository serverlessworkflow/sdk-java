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

import java.util.UUID;

public class WorkflowBuilder
    extends BaseWorkflowBuilder<WorkflowBuilder, DoTaskBuilder, TaskItemListBuilder> {

  private WorkflowBuilder(final String name, final String namespace, final String version) {
    super(name, namespace, version);
  }

  @Override
  protected DoTaskBuilder newDo() {
    return new DoTaskBuilder();
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

  @Override
  protected WorkflowBuilder self() {
    return this;
  }
}
