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
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import java.io.ByteArrayInputStream;
import java.time.Instant;

public class BytesBigMapPersistenceRestorer
    extends BigMapPersistenceRestorer<byte[], byte[], byte[], byte[]> {

  private final WorkflowBufferFactory factory;

  protected BytesBigMapPersistenceRestorer(
      BigMapPersistenceStore<String, byte[], byte[], byte[], byte[]> store,
      WorkflowBufferFactory factory) {
    super(store);
    this.factory = factory;
  }

  @Override
  protected PersistenceTaskInfo unmarshallTaskInfo(byte[] taskData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(taskData))) {
      buffer.readByte(); // version byte not used at the moment
      Instant date = buffer.readInstant();
      WorkflowModel model = (WorkflowModel) buffer.readObject();
      Boolean isEndNode = null;
      String nextPosition = null;
      isEndNode = buffer.readBoolean();
      boolean hasNext = buffer.readBoolean();
      if (hasNext) {
        nextPosition = buffer.readString();
      }
      return new PersistenceTaskInfo(date, model, isEndNode, nextPosition);
    }
  }

  @Override
  protected PersistenceInstanceInfo unmarshallInstanceInfo(byte[] instanceData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(instanceData))) {
      buffer.readByte(); // version byte not used at the moment
      return new PersistenceInstanceInfo(buffer.readInstant(), (WorkflowModel) buffer.readObject());
    }
  }

  @Override
  protected WorkflowModel unmarshallContext(byte[] contextData) {
    try (WorkflowInputBuffer buffer = factory.input(new ByteArrayInputStream(contextData))) {
      buffer.readByte(); // version byte not used at the moment
      return (WorkflowModel) buffer.readObject();
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
