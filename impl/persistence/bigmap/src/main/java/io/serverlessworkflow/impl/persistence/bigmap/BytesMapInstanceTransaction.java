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

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContextData;
import io.serverlessworkflow.impl.WorkflowInstanceData;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowStatus;
import io.serverlessworkflow.impl.executors.AbstractTaskExecutor;
import io.serverlessworkflow.impl.executors.TransitionInfo;
import io.serverlessworkflow.impl.marshaller.MarshallingUtils;
import io.serverlessworkflow.impl.marshaller.TaskStatus;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import io.serverlessworkflow.impl.persistence.CompletedTaskInfo;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceInfo;
import io.serverlessworkflow.impl.persistence.PersistenceTaskInfo;
import io.serverlessworkflow.impl.persistence.RetriedTaskInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

public abstract class BytesMapInstanceTransaction
    extends BigMapInstanceTransaction<byte[], byte[], byte[], byte[], byte[], byte[]> {

  private static final byte VERSION_0 = 0;
  private static final byte VERSION_1 = 1;
  private static final byte VERSION_2 = 2;
  private static final byte[] PROCESSED_VALUE = new byte[] {1};

  private final WorkflowBufferFactory factory;

  protected BytesMapInstanceTransaction(WorkflowBufferFactory factory) {
    this.factory = factory;
  }

  @Override
  protected byte[] marshallTaskCompleted(WorkflowContextData contextData, TaskContext taskContext) {

    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(VERSION_2);
      writer.writeEnum(TaskStatus.COMPLETED);
      writer.writeInstant(taskContext.completedAt());
      writeModel(writer, taskContext.output());
      writeModel(writer, contextData.context());
      TransitionInfo transition = taskContext.transition();
      writer.writeBoolean(transition.isEndNode());
      AbstractTaskExecutor<?> next = (AbstractTaskExecutor<?>) transition.next();
      if (next == null) {
        writer.writeBoolean(false);
      } else {
        writer.writeBoolean(true);
        writer.writeString(next.position().jsonPointer());
      }
      writer.writeInt(taskContext.iteration());
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected byte[] marshallStatus(WorkflowStatus status) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(VERSION_0);
      writer.writeEnum(status);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected byte[] marshallInstance(WorkflowInstanceData instance) {

    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(VERSION_0);
      writer.writeInstant(instance.startedAt());
      writeModel(writer, instance.input());
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void writeModel(WorkflowOutputBuffer writer, WorkflowModel model) {
    writer.writeObject(model);
  }

  protected byte[] marshallApplicationId(String id) {
    return MarshallingUtils.writeString(factory, id);
  }

  protected String unmarshallApplicationId(byte[] value) {
    return MarshallingUtils.readString(factory, value);
  }

  @Override
  protected byte[] marshallTaskRetried(
      WorkflowContextData workflowContext, TaskContext taskContext) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeByte(VERSION_1);
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
        case VERSION_0:
          return readVersion0(buffer);
        case VERSION_1:
          return readVersion1(buffer);
        case VERSION_2:
          return readVersion2(buffer);
      }
      throw new UnsupportedOperationException("Unknown version " + version);
    }
  }

  private PersistenceTaskInfo readVersion2(WorkflowInputBuffer buffer) {
    TaskStatus taskStatus = buffer.readEnum(TaskStatus.class);
    switch (taskStatus) {
      case COMPLETED:
        return new CompletedTaskInfo(
            buffer.readInstant(),
            (WorkflowModel) buffer.readObject(),
            (WorkflowModel) buffer.readObject(),
            buffer.readBoolean(),
            buffer.readBoolean() ? buffer.readString() : null,
            buffer.readInt());
      case RETRIED:
        return new RetriedTaskInfo(buffer.readShort());
    }
    throw new UnsupportedOperationException("Unknown status " + taskStatus);
  }

  private PersistenceTaskInfo readVersion1(WorkflowInputBuffer buffer) {
    TaskStatus taskStatus = buffer.readEnum(TaskStatus.class);
    switch (taskStatus) {
      case COMPLETED:
        return readVersion0(buffer);
      case RETRIED:
        return new RetriedTaskInfo(buffer.readShort());
    }
    throw new UnsupportedOperationException("Unknown status " + taskStatus);
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

  protected byte[] marshallCloudEvent(CloudEvent event) {
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkflowOutputBuffer writer = factory.output(bytes)) {
      writer.writeEnum(event.getSpecVersion());
      writer.writeString(event.getId());
      writer.writeString(event.getType());
      writer.writeString(event.getSource().toString());
      writer.writeObject(event.getSubject());
      writer.writeObject(event.getDataSchema());
      writer.writeObject(event.getDataContentType());
      writer.writeObject(event.getData() == null ? null : event.getData().toBytes());
      MarshallingUtils.writeCloudEventExtensions(writer, event);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected CloudEvent unmarshallCloudEvent(byte[] eventData) {
    try (ByteArrayInputStream bytes = new ByteArrayInputStream(eventData);
        WorkflowInputBuffer reader = factory.input(bytes)) {
      CloudEventBuilder builder =
          CloudEventBuilder.fromSpecVersion(reader.readEnum(SpecVersion.class));
      builder.withId(reader.readString());
      builder.withType(reader.readString());
      builder.withSource(URI.create(reader.readString()));
      builder.withSubject((String) reader.readObject());
      builder.withDataSchema((URI) reader.readObject());
      builder.withDataContentType((String) reader.readObject());
      builder.withData((byte[]) reader.readObject());
      MarshallingUtils.readCloudEventExtenstions(reader, eventData, builder);
      return builder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected byte[] processedValue() {
    return PROCESSED_VALUE;
  }
}
