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

import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceReader;
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import io.serverlessworkflow.impl.persistence.PersistenceWorkflowInfo;
import io.serverlessworkflow.impl.persistence.WorkflowPersistenceInstance;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BigMapInstanceReader<V, T, S> implements PersistenceInstanceReader {

  private final BigMapInstanceStore<String, V, T, S> store;

  protected BigMapInstanceReader(BigMapInstanceStore<String, V, T, S> store) {
    this.store = store;
  }

  private <Result> Result doTransaction(
      Function<BigMapInstanceTransaction<String, V, T, S>, Result> operations) {
    BigMapInstanceTransaction<String, V, T, S> transaction = store.begin();
    try {
      Result result = operations.apply(transaction);
      transaction.commit();
      return result;
    } catch (Exception ex) {
      transaction.rollback();
      throw ex;
    }
  }

  @Override
  public Map<String, WorkflowInstance> readAll(WorkflowDefinition definition) {
    return doTransaction(
        t -> {
          Map<String, V> instances = t.instanceData(definition);
          Map<String, S> status = t.status(definition);
          return instances.entrySet().stream()
              .map(
                  e ->
                      restore(
                          definition,
                          e.getKey(),
                          e.getValue(),
                          t.tasks(e.getKey()),
                          status.get(e.getKey())))
              .collect(Collectors.toMap(WorkflowInstance::id, i -> i));
        });
  }

  @Override
  public Map<String, WorkflowInstance> read(
      WorkflowDefinition definition, Collection<String> instanceIds) {
    return doTransaction(
        t -> {
          Map<String, V> instances = t.instanceData(definition);
          Map<String, S> status = t.status(definition);
          return instanceIds.stream()
              .map(id -> read(instances, status, t.tasks(id), definition, id))
              .flatMap(Optional::stream)
              .collect(Collectors.toMap(WorkflowInstance::id, id -> id));
        });
  }

  @Override
  public Optional<WorkflowInstance> read(WorkflowDefinition definition, String instanceId) {
    return doTransaction(
        t ->
            read(
                t.instanceData(definition),
                t.status(definition),
                t.tasks(instanceId),
                definition,
                instanceId));
  }

  private Optional<WorkflowInstance> read(
      Map<String, V> instances,
      Map<String, S> status,
      Map<String, T> tasks,
      WorkflowDefinition definition,
      String instanceId) {
    return instances.containsKey(instanceId)
        ? Optional.empty()
        : Optional.of(
            restore(
                definition, instanceId, instances.get(instanceId), tasks, status.get(instanceId)));
  }

  public void close() {}

  protected WorkflowInstance restore(
      WorkflowDefinition definition,
      String instanceId,
      V instanceData,
      Map<String, T> tasksData,
      S status) {
    return new WorkflowPersistenceInstance(
        definition, readPersistenceInfo(instanceId, instanceData, tasksData, status));
  }

  protected abstract PersistenceTaskInfo unmarshallTaskInfo(T taskData);

  protected abstract PersistenceInstanceInfo unmarshallInstanceInfo(V instanceData);

  protected abstract WorkflowStatus unmarshallStatus(S statusData);

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
}
