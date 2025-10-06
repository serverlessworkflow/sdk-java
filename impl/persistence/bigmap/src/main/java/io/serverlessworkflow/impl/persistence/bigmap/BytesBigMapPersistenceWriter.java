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
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import io.serverlessworkflow.impl.persistence.bigmap.MarshallingUtils.TaskStatus;
import java.io.ByteArrayOutputStream;

public class BytesBigMapPersistenceWriter
    extends BigMapIdPersistenceWriter<byte[], byte[], byte[]> {

  private final WorkflowBufferFactory factory;

  public BytesBigMapPersistenceWriter(
      BigMapPersistenceStore<String, byte[], byte[], byte[]> store, WorkflowBufferFactory factory) {
    super(store);
    this.factory = factory;
  }

  @Override
  protected byte[] marshallTaskCompleted(WorkflowContextData contextData, TaskContext taskContext) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(MarshallingUtils.VERSION_0);
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

  protected void writeTaskStatus(WorkflowOutputBuffer buffer, TaskStatus taskStatus) {
    buffer.writeByte((byte) taskStatus.ordinal());
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
}
