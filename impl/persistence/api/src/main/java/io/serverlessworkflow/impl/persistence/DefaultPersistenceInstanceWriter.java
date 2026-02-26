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

import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinitionData;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPersistenceInstanceWriter extends AbstractPersistenceInstanceWriter {

  private final PersistenceInstanceStore store;
  private final Map<String, CompletableFuture<Void>> futuresMap = new ConcurrentHashMap<>();
  private final Optional<ExecutorService> executorService;

  private static final Logger logger =
      LoggerFactory.getLogger(DefaultPersistenceInstanceWriter.class);

  protected DefaultPersistenceInstanceWriter(
      PersistenceInstanceStore store, Optional<ExecutorService> executorService) {
    this.store = store;
    this.executorService = executorService;
  }

  @Override
  protected CompletableFuture<Void> removeProcessInstance(WorkflowContextData workflowContext) {
    return super.removeProcessInstance(workflowContext)
        .thenRun(() -> futuresMap.remove(workflowContext.instanceData().id()));
  }

  @Override
  protected CompletableFuture<Void> doTransaction(
      Consumer<PersistenceInstanceOperations> operation, WorkflowContextData context) {
    final ExecutorService service =
        this.executorService.orElse(context.definition().application().executorService());
    final Runnable runnable = () -> executeTransaction(operation, context.definition());
    return futuresMap.compute(
        context.instanceData().id(),
        (k, v) ->
            v == null
                ? CompletableFuture.runAsync(runnable, service)
                : v.thenRunAsync(runnable, service));
  }

  private void executeTransaction(
      Consumer<PersistenceInstanceOperations> operation, WorkflowDefinitionData definition) {
    PersistenceInstanceTransaction transaction = store.begin();
    try {
      operation.accept(transaction);
      transaction.commit(definition);
    } catch (Exception ex) {
      try {
        transaction.rollback(definition);
      } catch (Exception rollEx) {
        logger.warn("Exception during rollback. Ignoring it", rollEx);
      }
      throw ex;
    }
  }

  @Override
  public void close() {
    futuresMap.clear();
  }
}
