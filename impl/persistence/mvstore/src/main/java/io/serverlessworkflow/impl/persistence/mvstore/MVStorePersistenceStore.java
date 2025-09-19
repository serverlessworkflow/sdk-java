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

import io.serverlessworkflow.impl.WorkflowDefinitionData;
import io.serverlessworkflow.impl.WorkflowId;
import io.serverlessworkflow.impl.persistence.bigmap.BytesBigMapPersistenceStore;
import java.util.Map;
import org.h2.mvstore.MVStore;

public class MVStorePersistenceStore extends BytesBigMapPersistenceStore {
  private final MVStore mvStore;

  public MVStorePersistenceStore(String dbName) {
    this.mvStore = MVStore.open(dbName);
  }

  @Override
  protected Map<String, byte[]> instances(WorkflowDefinitionData definition) {
    return mvStore.openMap(WorkflowId.asString(definition.workflow()));
  }

  @Override
  public void close() {
    if (!mvStore.isClosed()) {
      mvStore.close();
    }
  }
}
