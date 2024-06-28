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
package io.serverlessworkflow.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.serverlessworkflow.api.types.TaskItem;
import java.io.IOException;

class TaskItemSerializer extends JsonSerializer<TaskItem> {

  @Override
  public void serialize(TaskItem value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeStartObject();
    gen.writeObjectField(value.getName(), value.getTask());
    gen.writeEndObject();
  }
}
