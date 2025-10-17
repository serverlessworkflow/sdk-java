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

import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.persistence.CompletedTaskInfo;
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import io.serverlessworkflow.impl.persistence.RetriedTaskInfo;
import java.io.ByteArrayInputStream;
import java.time.Instant;

public class BytesMapInstanceReader extends BigMapInstanceReader<byte[], byte[], byte[]> {

  private final WorkflowBufferFactory factory;

  public BytesMapInstanceReader(
      BigMapInstanceStore<String, byte[], byte[], byte[]> store, WorkflowBufferFactory factory) {
    super(store);
    this.factory = factory;
  }

  @Override
  protected PersistenceTaskInfo unmarshallTaskInfo(byte[] taskData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(taskData))) {
      byte version = buffer.readByte();
      switch (version) {
        case MarshallingUtils.VERSION_0:
        default:
          return readVersion0(buffer);
        case MarshallingUtils.VERSION_1:
          return readVersion1(buffer);
      }
    }
  }

  private PersistenceTaskInfo readVersion1(WorkflowInputBuffer buffer) {
    TaskStatus taskStatus = buffer.readEnum(TaskStatus.class);
    switch (taskStatus) {
      case COMPLETED:
      default:
        return readVersion0(buffer);
      case RETRIED:
        return new RetriedTaskInfo(buffer.readShort());
    }
  }

  private PersistenceTaskInfo readVersion0(WorkflowInputBuffer buffer) {
    Instant date = buffer.readInstant();
    WorkflowModel model = (WorkflowModel) buffer.readObject();
    WorkflowModel context = (WorkflowModel) buffer.readObject();
    Boolean isEndNode = null;
    String nextPosition = null;
    isEndNode = buffer.readBoolean();
    boolean hasNext = buffer.readBoolean();
    if (hasNext) {
      nextPosition = buffer.readString();
    }
    return new CompletedTaskInfo(date, model, context, isEndNode, nextPosition);
  }

  @Override
  protected PersistenceInstanceInfo unmarshallInstanceInfo(byte[] instanceData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(instanceData))) {
      buffer.readByte(); // version byte not used at the moment
      return new PersistenceInstanceInfo(buffer.readInstant(), (WorkflowModel) buffer.readObject());
    }
  }

  @Override
  protected WorkflowStatus unmarshallStatus(byte[] statusData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(statusData))) {
      buffer.readByte(); // version byte not used at the moment
      return buffer.readEnum(WorkflowStatus.class);
    }
  }
}
