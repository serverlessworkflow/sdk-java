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
package io.serverlessworkflow.impl.persistence.mvstore;

import io.serverlessworkflow.impl.marshaller.DefaultBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceStore;
import io.serverlessworkflow.impl.persistence.bigmap.BigMapInstanceTransaction;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.tx.TransactionStore;

public class MVStorePersistenceStore implements PersistenceInstanceStore {
  private final TransactionStore mvStore;
  private WorkflowBufferFactory factory;

  public MVStorePersistenceStore(String dbName) {
    this(dbName, DefaultBufferFactory.factory());
  }

  public MVStorePersistenceStore(String dbName, WorkflowBufferFactory factory) {
    this.mvStore = new TransactionStore(MVStore.open(dbName));
    this.factory = factory;
  }

  @Override
  public void close() {
    mvStore.close();
  }

  @Override
  public BigMapInstanceTransaction<byte[], byte[], byte[]> begin() {
    return new MVStoreTransaction(mvStore.begin(), factory);
  }
}
