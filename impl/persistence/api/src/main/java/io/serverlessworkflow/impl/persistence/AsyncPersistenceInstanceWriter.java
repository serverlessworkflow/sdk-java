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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsyncPersistenceInstanceWriter extends AbstractPersistenceInstanceWriter {

  private static final Logger logger =
      LoggerFactory.getLogger(AsyncPersistenceInstanceWriter.class);

  private final Map<String, CompletableFuture<Void>> futuresMap = new ConcurrentHashMap<>();

  @Override
  protected CompletableFuture<Void> doTransaction(
      Consumer<PersistenceInstanceOperations> operation, WorkflowContextData context) {
    final ExecutorService service =
        executorService().orElse(context.definition().application().executorService());
    final Runnable runnable = () -> doTransaction(operation, context.definition());
    return futuresMap.compute(
        context.instanceData().id(),
        (k, v) ->
            v == null
                ? CompletableFuture.runAsync(runnable, service)
                : v.thenRunAsync(runnable, service));
  }

  @Override
  protected CompletableFuture<Void> removeProcessInstance(WorkflowContextData workflowContext) {
    return super.removeProcessInstance(workflowContext)
        .thenRun(() -> futuresMap.remove(workflowContext.instanceData().id()));
  }

  protected abstract void doTransaction(
      Consumer<PersistenceInstanceOperations> operation, WorkflowDefinitionData definition);

  protected Optional<ExecutorService> executorService() {
    return Optional.empty();
  }

  @Override
  public void close() {
    for (CompletableFuture<Void> future : futuresMap.values()) {
      try {
        future.get();
      } catch (InterruptedException ex) {
        logger.warn("Thread interrupted while writing to db", ex);
        Thread.currentThread().interrupt();
      } catch (ExecutionException ex) {
        logger.warn("Exception while writing to db", ex.getCause());
      }
    }
    futuresMap.clear();
  }
}
