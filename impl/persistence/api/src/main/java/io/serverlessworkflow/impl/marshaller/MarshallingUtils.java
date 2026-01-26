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
package io.serverlessworkflow.impl.marshaller;

import io.serverlessworkflow.impl.WorkflowModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MarshallingUtils {

  private MarshallingUtils() {}

  public static byte[] writeInstant(WorkflowBufferFactory factory, Instant instant) {
    return writeValue(factory, instant, (b, v) -> b.writeInstant(v));
  }

  public static <T extends Enum<T>> byte[] writeEnum(
      WorkflowBufferFactory factory, T enumInstance) {
    return writeValue(factory, enumInstance, (b, v) -> b.writeEnum(v));
  }

  public static byte[] writeModel(WorkflowBufferFactory factory, WorkflowModel model) {
    return writeValue(factory, model, (b, v) -> b.writeObject(v));
  }

  public static byte[] writeShort(WorkflowBufferFactory factory, short value) {
    return writeValue(factory, value, (b, v) -> b.writeShort(v));
  }

  public static byte[] writeBoolean(WorkflowBufferFactory factory, boolean value) {
    return writeValue(factory, value, (b, v) -> b.writeBoolean(v));
  }

  public static byte[] writeString(WorkflowBufferFactory factory, String value) {
    return writeValue(factory, value, (b, v) -> b.writeString(v));
  }

  public static String readString(WorkflowBufferFactory factory, byte[] value) {
    return readValue(factory, value, WorkflowInputBuffer::readString);
  }

  public static boolean readBoolean(WorkflowBufferFactory factory, byte[] value) {
    return readValue(factory, value, WorkflowInputBuffer::readBoolean);
  }

  public static short readShort(WorkflowBufferFactory factory, byte[] value) {
    return readValue(factory, value, WorkflowInputBuffer::readShort);
  }

  public static WorkflowModel readModel(WorkflowBufferFactory factory, byte[] value) {
    return readValue(factory, value, b -> (WorkflowModel) b.readObject());
  }

  public static Instant readInstant(WorkflowBufferFactory factory, byte[] value) {
    return readValue(factory, value, WorkflowInputBuffer::readInstant);
  }

  public static <T extends Enum<T>> T readEnum(
      WorkflowBufferFactory factory, byte[] value, Class<T> enumClass) {
    return readValue(factory, value, b -> b.readEnum(enumClass));
  }

  private static <T> byte[] writeValue(
      WorkflowBufferFactory factory, T value, BiConsumer<WorkflowOutputBuffer, T> valueConsumer) {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer buffer = factory.output(bytesOut)) {
      valueConsumer.accept(buffer, value);
    }
    return bytesOut.toByteArray();
  }

  private static <T> T readValue(
      WorkflowBufferFactory factory, byte[] value, Function<WorkflowInputBuffer, T> valueConsumer) {
    if (value == null) {
      return null;
    }
    ByteArrayInputStream bytesIn = new ByteArrayInputStream(value);
    try (WorkflowInputBuffer buffer = factory.input(bytesIn)) {
      return valueConsumer.apply(buffer);
    }
  }
}
