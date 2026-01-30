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
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.persistence.bigmap.BytesMapInstanceTransaction;
import java.util.Map;
import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;

public class MVStoreTransaction extends BytesMapInstanceTransaction {

  protected static final String ID_SEPARATOR = "-";

  private final Transaction transaction;

  public MVStoreTransaction(Transaction transaction, WorkflowBufferFactory factory) {
    super(factory);
    this.transaction = transaction;
  }

  protected static String identifier(Workflow workflow, String sep) {
    Document document = workflow.getDocument();
    return document.getNamespace() + sep + document.getName() + sep + document.getVersion();
  }

  @Override
  public Map<String, byte[]> instanceData(WorkflowDefinitionData workflowContext) {
    return openMap(workflowContext, "instances");
  }

  @Override
  public Map<String, byte[]> tasks(String instanceId) {
    return taskMap(instanceId);
  }

  @Override
  public Map<String, byte[]> status(WorkflowDefinitionData workflowContext) {
    return openMap(workflowContext, "status");
  }

  @Override
  public void removeTasks(String instanceId) {
    transaction.removeMap(taskMap(instanceId));
  }

  private TransactionMap<String, byte[]> taskMap(String instanceId) {
    return transaction.openMap(mapTaskName(instanceId));
  }

  private Map<String, byte[]> openMap(WorkflowDefinitionData workflowDefinition, String suffix) {
    return transaction.openMap(
        identifier(workflowDefinition.workflow(), ID_SEPARATOR) + ID_SEPARATOR + suffix);
  }

  private String mapTaskName(String instanceId) {
    return instanceId + ID_SEPARATOR + "tasks";
  }

  @Override
  public void commit(WorkflowDefinitionData definition) {
    transaction.commit();
  }

  @Override
  public void rollback(WorkflowDefinitionData definition) {
    transaction.rollback();
  }

  @Override
  protected Map<String, byte[]> applicationData() {
    return transaction.openMap("APPLICATION");
  }
}
