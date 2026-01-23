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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowDefinitionData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceInfo;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceTransaction;
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import io.serverlessworkflow.impl.persistence.PersistenceWorkflowInfo;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BigMapInstanceTransaction<K, V, T, S>
    implements PersistenceInstanceTransaction<K> {

  @Override
  public void writeInstanceData(K key, WorkflowContextData workflowContext) {
    instanceData(workflowContext.definition())
        .put(key, marshallInstance(workflowContext.instanceData()));
  }

  @Override
  public void writeRetryTask(
      K key, WorkflowContextData workflowContext, TaskContextData taskContext) {
    tasks(key)
        .put(
            taskContext.position().jsonPointer(),
            marshallTaskRetried(workflowContext, (TaskContext) taskContext));
  }

  @Override
  public void writeCompletedTask(
      K key, WorkflowContextData workflowContext, TaskContextData taskContext) {
    tasks(key)
        .put(
            taskContext.position().jsonPointer(),
            marshallTaskRetried(workflowContext, (TaskContext) taskContext));
  }

  @Override
  public Stream<PersistenceWorkflowInfo> scanAll(WorkflowDefinition definition) {
    Map<K, V> instances = instanceData(definition);
    Map<K, S> status = status(definition);
    return instances.entrySet().stream()
        .map(
            e ->
                readPersistenceInfo(
                    e.getKey(), e.getValue(), tasks(e.getKey()), status.get(e.getKey())));
  }

  @Override
  public Optional<PersistenceWorkflowInfo> readWorkflowInfo(WorkflowDefinition definition, K key) {
    Map<K, V> instances = instanceData(definition);
    return instances.containsKey(key)
        ? Optional.empty()
        : Optional.of(
            readPersistenceInfo(key, instances.get(key), tasks(key), status(definition).get(key)));
  }

  @Override
  public void writeStatus(K key, WorkflowStatus status, WorkflowContextData workflowContext) {
    status(workflowContext.definition()).put(key, marshallStatus(status));
  }

  public void removeInstanceData(K key, WorkflowContextData workflowContext) {
    instanceData(workflowContext.definition()).remove(key);
  }

  public void removeStatus(K key, WorkflowContextData workflowContext) {
    status(workflowContext.definition()).remove(key);
  }

  protected PersistenceWorkflowInfo readPersistenceInfo(
      K instanceId, V instanceData, Map<String, T> tasksData, S status) {
    PersistenceInstanceInfo instanceInfo = unmarshallInstanceInfo(instanceData);
    return new PersistenceWorkflowInfo(
        instanceId.toString(),
        instanceInfo.startedAt(),
        instanceInfo.input(),
        status == null ? null : unmarshallStatus(status),
        tasksData.entrySet().stream()
            .collect(
                Collectors.toMap(Entry::getKey, entry -> unmarshallTaskInfo(entry.getValue()))));
  }

  protected abstract Map<K, V> instanceData(WorkflowDefinitionData definition);

  protected abstract Map<K, S> status(WorkflowDefinitionData workflowContext);

  protected abstract Map<String, T> tasks(K instanceId);

  protected abstract V marshallInstance(WorkflowInstanceData instance);

  protected abstract T marshallTaskCompleted(
      WorkflowContextData workflowContext, TaskContext taskContext);

  protected abstract T marshallTaskRetried(
      WorkflowContextData workflowContext, TaskContext taskContext);

  protected abstract S marshallStatus(WorkflowStatus status);

  protected abstract PersistenceTaskInfo unmarshallTaskInfo(T taskData);

  protected abstract PersistenceInstanceInfo unmarshallInstanceInfo(V instanceData);

  protected abstract WorkflowStatus unmarshallStatus(S statusData);
}
