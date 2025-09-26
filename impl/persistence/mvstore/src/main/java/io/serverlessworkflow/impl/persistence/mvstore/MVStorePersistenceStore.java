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

import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowDefinitionData;
import io.serverlessworkflow.impl.persistence.bigmap.BytesBigMapPersistenceStore;
import java.util.Map;
import org.h2.mvstore.MVStore;

public class MVStorePersistenceStore extends BytesBigMapPersistenceStore {
  private final MVStore mvStore;

  protected static final String ID_SEPARATOR = "-";

  public MVStorePersistenceStore(String dbName) {
    this.mvStore = MVStore.open(dbName);
  }

  protected static String identifier(Workflow workflow, String sep) {
    Document document = workflow.getDocument();
    return document.getNamespace() + sep + document.getName() + sep + document.getVersion();
  }

  @Override
  protected Map<String, byte[]> instances(WorkflowDefinitionData definition) {
    return mvStore.openMap(identifier(definition.workflow(), ID_SEPARATOR));
  }

  @Override
  public void close() {
    if (!mvStore.isClosed()) {
      mvStore.close();
    }
  }
}
