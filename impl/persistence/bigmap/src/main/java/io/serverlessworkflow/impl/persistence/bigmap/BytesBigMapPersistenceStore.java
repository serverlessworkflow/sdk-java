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

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowMutableInstance;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

public abstract class BytesBigMapPersistenceStore extends BigMapIdPersistenceStore<byte[]> {

  private final WorkflowBufferFactory factory;

  public BytesBigMapPersistenceStore(WorkflowBufferFactory factory) {
    this.factory = factory;
  }

  public BytesBigMapPersistenceStore() {
    this(new DefaultBufferFactory());
  }

  @Override
  protected WorkflowInstance unmarshall(WorkflowDefinition definition, byte[] bytes) {

    try (WorkflowInputBuffer reader = factory.input(new ByteArrayInputStream(bytes))) {
      String id = reader.readString();
      WorkflowStatus status = reader.readEnum(WorkflowStatus.class);
      WorkflowModel inputModel = readModel(bytes);
      Instant startDate = reader.readInstant();
      WorkflowMutableInstance instance =
          new WorkflowMutableInstance(definition, id, inputModel, status);
      WorkflowModel context = readModel(bytes);
      WorkflowPosition position = readPosition(bytes);
      WorkflowModel model = readModel(bytes);
      instance.restore(position, model, context, startDate);
      return instance;
    }
  }

  private WorkflowPosition readPosition(byte[] bytes) {
    // TODO read position
    return null;
  }

  private void writePosition(WorkflowPosition position) {
    // TODO write position
  }

  private WorkflowModel readModel(byte[] value) {
    // TODO read model
    return null;
  }

  private void writeModel(WorkflowModel model) {
    // TODO write model
  }

  @Override
  protected byte[] marshall(WorkflowContextData workflowContext, TaskContextData taskContext) {

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      WorkflowInstanceData instance = workflowContext.instanceData();
      writer.writeString(instance.id());
      writer.writeEnum(instance.status());
      writeModel(workflowContext.instanceData().input());
      writer.writeInstant(instance.startedAt());
      writeModel(workflowContext.context());
      writePosition(taskContext.position());
      writeModel(taskContext.input());
    }
    return bytes.toByteArray();
  }
}
