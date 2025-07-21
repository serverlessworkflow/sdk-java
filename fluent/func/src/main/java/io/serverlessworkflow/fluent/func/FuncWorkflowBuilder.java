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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.fluent.spec.BaseWorkflowBuilder;
import java.util.UUID;

public class FuncWorkflowBuilder
    extends BaseWorkflowBuilder<FuncWorkflowBuilder, FuncDoTaskBuilder, FuncTaskItemListBuilder>
    implements FuncTransformations<FuncWorkflowBuilder> {

  private FuncWorkflowBuilder(final String name, final String namespace, final String version) {
    super(name, namespace, version);
  }

  public static FuncWorkflowBuilder workflow(final String name, final String namespace) {
    return new FuncWorkflowBuilder(name, namespace, DEFAULT_VERSION);
  }

  public static FuncWorkflowBuilder workflow(final String name) {
    return new FuncWorkflowBuilder(name, DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  public static FuncWorkflowBuilder workflow() {
    return new FuncWorkflowBuilder(
        UUID.randomUUID().toString(), DEFAULT_NAMESPACE, DEFAULT_VERSION);
  }

  @Override
  protected FuncDoTaskBuilder newDo() {
    return new FuncDoTaskBuilder();
  }

  @Override
  protected FuncWorkflowBuilder self() {
    return this;
  }
}
