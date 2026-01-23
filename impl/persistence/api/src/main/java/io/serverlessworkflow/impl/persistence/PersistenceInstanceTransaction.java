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
package io.serverlessworkflow.impl.persistence;

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.util.Optional;
import java.util.stream.Stream;

public interface PersistenceInstanceTransaction<K> {

  void commit();

  void rollback();

  void writeInstanceData(K key, WorkflowContextData workflowContext);

  void writeRetryTask(K key, WorkflowContextData workflowContext, TaskContextData taskContext);

  void writeCompletedTask(K key, WorkflowContextData workflowContext, TaskContextData taskContext);

  void writeStatus(K key, WorkflowStatus suspended, WorkflowContextData workflowContext);

  void removeInstanceData(K key, WorkflowContextData workflowContext);

  void removeStatus(K key, WorkflowContextData workflowContext);

  void removeTasks(K instanceId);

  Stream<PersistenceWorkflowInfo> scanAll(WorkflowDefinition definition);

  Optional<PersistenceWorkflowInfo> readWorkflowInfo(WorkflowDefinition definition, K key);
}
