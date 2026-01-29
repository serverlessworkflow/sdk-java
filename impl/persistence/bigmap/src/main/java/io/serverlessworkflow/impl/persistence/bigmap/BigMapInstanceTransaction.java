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

public abstract class BigMapInstanceTransaction<V, T, S, A>
    implements PersistenceInstanceTransaction {

  @Override
  public void writeInstanceData(WorkflowContextData workflowContext) {
    String key = key(workflowContext);
    instanceData(workflowContext.definition())
        .put(key, marshallInstance(workflowContext.instanceData()));
    applicationData()
        .put(key, marshallApplicationId(workflowContext.definition().application().id()));
  }

  @Override
  public void writeRetryTask(WorkflowContextData workflowContext, TaskContextData taskContext) {
    tasks(key(workflowContext))
        .put(
            taskContext.position().jsonPointer(),
            marshallTaskRetried(workflowContext, (TaskContext) taskContext));
  }

  @Override
  public void writeCompletedTask(WorkflowContextData workflowContext, TaskContextData taskContext) {
    tasks(key(workflowContext))
        .put(
            taskContext.position().jsonPointer(),
            marshallTaskCompleted(workflowContext, (TaskContext) taskContext));
  }

  @Override
  public Stream<PersistenceWorkflowInfo> scanAll(
      String applicationId, WorkflowDefinition definition) {
    Map<String, V> instances = instanceData(definition);
    Map<String, A> applicationData = applicationData();
    Map<String, S> status = status(definition);
    return instances.entrySet().stream()
        .filter(e -> testAppl(applicationData, e.getKey(), applicationId))
        .map(
            e ->
                readPersistenceInfo(
                    e.getKey(), e.getValue(), tasks(e.getKey()), status.get(e.getKey())));
  }

  private boolean testAppl(Map<String, A> applicationData, String key, String applicationId) {
    A item = applicationData.get(key);
    return item == null || unmarshallApplicationId(item).equals(applicationId);
  }

  @Override
  public Optional<PersistenceWorkflowInfo> readWorkflowInfo(
      WorkflowDefinition definition, String key) {
    Map<String, V> instances = instanceData(definition);
    return instances.containsKey(key)
        ? Optional.of(
            readPersistenceInfo(key, instances.get(key), tasks(key), status(definition).get(key)))
        : Optional.empty();
  }

  @Override
  public void writeStatus(WorkflowContextData workflowContext, WorkflowStatus status) {
    status(workflowContext.definition()).put(key(workflowContext), marshallStatus(status));
  }

  @Override
  public void removeProcessInstance(WorkflowContextData workflowContext) {
    String key = key(workflowContext);
    WorkflowDefinitionData definition = workflowContext.definition();
    instanceData(definition).remove(key);
    clearStatus(definition, key);
    removeTasks(key);
  }

  @Override
  public void clearStatus(WorkflowContextData workflowContext) {
    clearStatus(workflowContext.definition(), key(workflowContext));
  }

  private void clearStatus(WorkflowDefinitionData definition, String key) {
    status(definition).remove(key);
  }

  protected PersistenceWorkflowInfo readPersistenceInfo(
      String instanceId, V instanceData, Map<String, T> tasksData, S status) {
    PersistenceInstanceInfo instanceInfo = unmarshallInstanceInfo(instanceData);
    return new PersistenceWorkflowInfo(
        instanceId,
        instanceInfo.startedAt(),
        instanceInfo.input(),
        status == null ? null : unmarshallStatus(status),
        tasksData.entrySet().stream()
            .collect(
                Collectors.toMap(Entry::getKey, entry -> unmarshallTaskInfo(entry.getValue()))));
  }

  private String key(WorkflowContextData workflowContext) {
    return workflowContext.instanceData().id();
  }

  protected abstract Map<String, A> applicationData();

  protected abstract Map<String, V> instanceData(WorkflowDefinitionData definition);

  protected abstract Map<String, S> status(WorkflowDefinitionData workflowContext);

  protected abstract Map<String, T> tasks(String instanceId);

  protected abstract V marshallInstance(WorkflowInstanceData instance);

  protected abstract T marshallTaskCompleted(
      WorkflowContextData workflowContext, TaskContext taskContext);

  protected abstract T marshallTaskRetried(
      WorkflowContextData workflowContext, TaskContext taskContext);

  protected abstract A marshallApplicationId(String id);

  protected abstract S marshallStatus(WorkflowStatus status);

  protected abstract PersistenceTaskInfo unmarshallTaskInfo(T taskData);

  protected abstract PersistenceInstanceInfo unmarshallInstanceInfo(V instanceData);

  protected abstract WorkflowStatus unmarshallStatus(S statusData);

  protected abstract String unmarshallApplicationId(A a);

  protected abstract void removeTasks(String key);
}
