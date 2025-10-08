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

import static io.serverlessworkflow.impl.WorkflowUtils.safeClose;

import io.serverlessworkflow.impl.marshaller.DefaultBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceReader;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceWriter;

public class BytesMapPersistenceInstanceHandlers extends PersistenceInstanceHandlers
    implements AutoCloseable {

  private final BigMapInstanceStore<String, byte[], byte[], byte[]> store;

  protected BytesMapPersistenceInstanceHandlers(
      PersistenceInstanceWriter writer,
      PersistenceInstanceReader reader,
      BigMapInstanceStore<String, byte[], byte[], byte[]> store) {
    super(writer, reader);
    this.store = store;
  }

  public static class Builder {
    private final BigMapInstanceStore<String, byte[], byte[], byte[]> store;
    private WorkflowBufferFactory factory;

    private Builder(BigMapInstanceStore<String, byte[], byte[], byte[]> store) {
      this.store = store;
    }

    public Builder withFactory(WorkflowBufferFactory factory) {
      this.factory = factory;
      return this;
    }

    public PersistenceInstanceHandlers build() {
      if (factory == null) {
        factory = DefaultBufferFactory.factory();
      }
      return new BytesMapPersistenceInstanceHandlers(
          new BytesMapInstanceWriter(store, factory),
          new BytesMapInstanceReader(store, factory),
          store);
    }
  }

  public static Builder builder(BigMapInstanceStore<String, byte[], byte[], byte[]> store) {
    return new Builder(store);
  }

  @Override
  public void close() {
    super.close();
    safeClose(store);
  }
}
