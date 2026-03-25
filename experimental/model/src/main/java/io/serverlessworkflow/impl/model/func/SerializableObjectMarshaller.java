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
package io.serverlessworkflow.impl.model.func;

import io.serverlessworkflow.impl.marshaller.CustomObjectMarshaller;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;

public class SerializableObjectMarshaller implements CustomObjectMarshaller<Serializable> {

  @Override
  public void write(WorkflowOutputBuffer buffer, Serializable object) {
    try (ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytesOut)) {
      out.writeObject(object);
      buffer.writeBytes(bytesOut.toByteArray());
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    }
  }

  @Override
  public Serializable read(WorkflowInputBuffer buffer, Class<? extends Serializable> objectClass) {
    try (ByteArrayInputStream bytesIn = new ByteArrayInputStream(buffer.readBytes());
        ObjectInputStream in = new ObjectInputStream(bytesIn)) {
      return objectClass.cast(in.readObject());
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    } catch (ClassNotFoundException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public Class<Serializable> getObjectClass() {
    return Serializable.class;
  }

  @Override
  public int priority() {
    return Integer.MAX_VALUE;
  }
}
