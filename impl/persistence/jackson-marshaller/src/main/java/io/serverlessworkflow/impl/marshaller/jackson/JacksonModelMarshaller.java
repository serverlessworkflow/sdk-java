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
package io.serverlessworkflow.impl.marshaller.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.serverlessworkflow.impl.expressions.jq.JacksonModel;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import io.serverlessworkflow.impl.marshaller.CustomObjectMarshaller;
import io.serverlessworkflow.impl.marshaller.WorkflowInputBuffer;
import io.serverlessworkflow.impl.marshaller.WorkflowOutputBuffer;
import java.io.IOException;
import java.io.UncheckedIOException;

public class JacksonModelMarshaller implements CustomObjectMarshaller<JacksonModel> {

  @Override
  public void write(WorkflowOutputBuffer buffer, JacksonModel object) {
    try {
      buffer.writeBytes(JsonUtils.mapper().writeValueAsBytes(object));
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public JacksonModel read(WorkflowInputBuffer buffer) {
    try {
      return JsonUtils.mapper().readValue(buffer.readBytes(), JacksonModel.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Class<JacksonModel> getObjectClass() {
    return JacksonModel.class;
  }
}
