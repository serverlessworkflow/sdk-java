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
package io.serverlessworkflow.impl.model.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.marshaller.DefaultBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class JacksonModelSerializationTest {

  @Test
  void testObject() throws IOException {
    testMarshallUnMarshall(new Employee("Mortadelo", "TIA"));
  }

  @Test
  void testModel() throws IOException {
    testMarshallUnMarshall(
        new JacksonModel(JsonUtils.mapper().createObjectNode().put("Mortadelo", "TIA")));
  }

  private void testMarshallUnMarshall(Object object) {
    WorkflowBufferFactory factory = DefaultBufferFactory.factory();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (WorkflowOutputBuffer writer = factory.output(output)) {
      writer.writeObject(object);
    }
    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    try (WorkflowInputBuffer reader = factory.input(input)) {
      assertThat(reader.readObject()).isEqualTo(object);
    }
  }
}
