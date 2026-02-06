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

import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import java.util.Optional;
import java.util.stream.Stream;

public class DefaultPersistenceInstanceReader implements PersistenceInstanceReader {

  private final PersistenceInstanceStore store;

  protected DefaultPersistenceInstanceReader(PersistenceInstanceStore store) {
    this.store = store;
  }

  @Override
  public Optional<WorkflowInstance> find(WorkflowDefinition definition, String instanceId) {
    PersistenceInstanceTransaction transaction = store.begin();
    try {
      Optional<WorkflowInstance> instance = read(transaction, definition, instanceId);
      transaction.commit(definition);
      return instance;
    } catch (Exception ex) {
      transaction.rollback(definition);
      throw ex;
    }
  }

  private Optional<WorkflowInstance> read(
      PersistenceInstanceTransaction t, WorkflowDefinition definition, String instanceId) {
    return t.readWorkflowInfo(definition, instanceId)
        .map(i -> new WorkflowPersistenceInstance(definition, i));
  }

  @Override
  public Stream<WorkflowInstance> scanAll(WorkflowDefinition definition, String applicationId) {
    PersistenceInstanceTransaction transaction = store.begin();
    return transaction
        .scanAll(applicationId, definition)
        .onClose(() -> transaction.commit(definition))
        .map(v -> new WorkflowPersistenceInstance(definition, v));
  }

  @Override
  public void close() {}
}
