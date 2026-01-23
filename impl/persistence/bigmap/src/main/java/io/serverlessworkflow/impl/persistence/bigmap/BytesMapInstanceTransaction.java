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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.executors.AbstractTaskExecutor;
import io.serverlessworkflow.impl.executors.TaskExecutor;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import io.serverlessworkflow.impl.persistence.CompletedTaskInfo;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceInfo;
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import io.serverlessworkflow.impl.persistence.RetriedTaskInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public abstract class BytesMapInstanceTransaction
    extends BigMapInstanceTransaction<String, byte[], byte[], byte[]> {

  private final WorkflowBufferFactory factory;

  protected BytesMapInstanceTransaction(WorkflowBufferFactory factory) {
    this.factory = factory;
  }

  @Override
  protected byte[] marshallTaskCompleted(WorkflowContextData contextData, TaskContext taskContext) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(MarshallingUtils.VERSION_1);
      writer.writeEnum(TaskStatus.COMPLETED);
      writer.writeInstant(taskContext.completedAt());
      writeModel(writer, taskContext.output());
      writeModel(writer, contextData.context());
      boolean isEndNode = taskContext.transition().isEndNode();
      writer.writeBoolean(isEndNode);
      TaskExecutor<?> next = taskContext.transition().next();
      if (next == null) {
        writer.writeBoolean(false);
      } else {
        writer.writeBoolean(true);
        writer.writeString(((AbstractTaskExecutor) next).position().jsonPointer());
      }
    }

    return bytes.toByteArray();
  }

  @Override
  protected byte[] marshallStatus(WorkflowStatus status) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(MarshallingUtils.VERSION_0);
      writer.writeEnum(status);
    }
    return bytes.toByteArray();
  }

  @Override
  protected byte[] marshallInstance(WorkflowInstanceData instance) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(MarshallingUtils.VERSION_0);
      writer.writeInstant(instance.startedAt());
      writeModel(writer, instance.input());
    }
    return bytes.toByteArray();
  }

  protected void writeModel(WorkflowOutputBuffer writer, WorkflowModel model) {
    writer.writeObject(model);
  }

  @Override
  protected byte[] marshallTaskRetried(
      WorkflowContextData workflowContext, TaskContext taskContext) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(MarshallingUtils.VERSION_1);
      writer.writeEnum(TaskStatus.RETRIED);
      writer.writeShort(taskContext.retryAttempt());
    }
    return bytes.toByteArray();
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
    return new CompletedTaskInfo(
        buffer.readInstant(),
        (WorkflowModel) buffer.readObject(),
        (WorkflowModel) buffer.readObject(),
        buffer.readBoolean(),
        buffer.readBoolean() ? buffer.readString() : null);
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
