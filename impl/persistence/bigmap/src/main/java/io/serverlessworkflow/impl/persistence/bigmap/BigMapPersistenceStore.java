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

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowDefinitionData;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.persistence.WorkflowMinimumPersistenceReader;
import io.serverlessworkflow.impl.persistence.WorkflowPersistenceWriter;
import java.util.Map;
import java.util.stream.Stream;

public abstract class BigMapPersistenceStore<K, V>
    implements WorkflowMinimumPersistenceReader, WorkflowPersistenceWriter {

  @Override
  public void started(WorkflowContextData workflowContext) {
    instances(workflowContext.definition()).put(key(workflowContext), marshall(workflowContext));
  }

  @Override
  public void completed(WorkflowContextData workflowContext) {
    instances(workflowContext.definition()).remove(workflowContext.instanceData().id());
  }

  @Override
  public void failed(WorkflowContextData workflowContext, Throwable ex) {
    instances(workflowContext.definition()).remove(workflowContext.instanceData().id());
  }

  @Override
  public void aborted(WorkflowContextData workflowContext) {
    instances(workflowContext.definition()).remove(workflowContext.instanceData().id());
  }

  @Override
  public void updated(WorkflowContextData workflowContext, TaskContextData taskContext) {
    instances(workflowContext.definition()).put(key(workflowContext), marshall(workflowContext));
  }

  @Override
  public void suspended(WorkflowContextData workflowContext) {
    // nothing to do
  }

  @Override
  public void resumed(WorkflowContextData workflowContext) {
    // nothing to do
  }

  @Override
  public Stream<WorkflowInstance> all(WorkflowDefinition definition) {
    return instances(definition).values().stream().map(value -> unmarshall(definition, value));
  }

  protected abstract WorkflowInstance unmarshall(WorkflowDefinition definition, V value);

  protected abstract K key(WorkflowContextData workflowContext);

  protected abstract Map<K, V> instances(WorkflowDefinitionData definition);

  protected abstract V marshall(WorkflowContextData workflowContext, TaskContextData taskContext);

  protected V marshall(WorkflowContextData workflowContext) {
    return marshall(workflowContext, null);
  }
}
