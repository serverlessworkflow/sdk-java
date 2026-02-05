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

import static io.serverlessworkflow.impl.WorkflowUtils.safeClose;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class DefaultPersistenceInstanceHandlers extends PersistenceInstanceHandlers {

  public static class Builder {

    private final PersistenceInstanceStore store;
    private ExecutorService executorService;
    private Duration closeTimeout;

    private Builder(PersistenceInstanceStore store) {
      this.store = store;
    }

    public Builder withExecutorService(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public Builder withCloseTimeout(Duration closeTimeout) {
      this.closeTimeout = closeTimeout;
      return this;
    }

    public PersistenceInstanceHandlers build() {
      return new DefaultPersistenceInstanceHandlers(
          new DefaultPersistenceInstanceWriter(
              store,
              Optional.ofNullable(executorService),
              closeTimeout == null ? Duration.ofSeconds(1) : closeTimeout),
          new DefaultPersistenceInstanceReader(store),
          store);
    }
  }

  public static Builder builder(PersistenceInstanceStore store) {
    return new Builder(store);
  }

  public static PersistenceInstanceHandlers from(PersistenceInstanceStore store) {
    return new Builder(store).build();
  }

  private final PersistenceInstanceStore store;

  private DefaultPersistenceInstanceHandlers(
      PersistenceInstanceWriter writer,
      PersistenceInstanceReader reader,
      PersistenceInstanceStore store) {
    super(writer, reader);
    this.store = store;
  }

  @Override
  public void close() {
    super.close();
    safeClose(store);
  }
}
