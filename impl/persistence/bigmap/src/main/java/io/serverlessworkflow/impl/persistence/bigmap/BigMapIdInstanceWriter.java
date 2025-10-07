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
package io.serverlessworkflow.impl.persistence.bigmap;

import io.serverlessworkflow.impl.WorkflowContextData;

public abstract class BigMapIdInstanceWriter<V, T, S>
    extends BigMapInstanceWriter<String, V, T, S> {

  protected BigMapIdInstanceWriter(BigMapInstanceStore<String, V, T, S> store) {
    super(store);
  }

  @Override
  protected String key(WorkflowContextData workflowContext) {
    return workflowContext.instanceData().id();
  }
}
