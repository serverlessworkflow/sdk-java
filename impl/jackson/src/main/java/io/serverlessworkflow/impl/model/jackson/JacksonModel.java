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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.jackson.JsonUtils;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@JsonSerialize(using = JacksonModelSerializer.class)
@JsonDeserialize(using = JacksonModelDeserializer.class)
public class JacksonModel implements WorkflowModel {

  protected JsonNode node;

  public static final JacksonModel TRUE = new JacksonModel(BooleanNode.TRUE);
  public static final JacksonModel FALSE = new JacksonModel(BooleanNode.FALSE);
  public static final JacksonModel NULL = new JacksonModel(NullNode.instance);

  JacksonModel(JsonNode node) {
    this.node = node;
  }

  @Override
  public Optional<Boolean> asBoolean() {
    return node.isBoolean() ? Optional.of(node.asBoolean()) : Optional.empty();
  }

  @Override
  public Collection<WorkflowModel> asCollection() {
    return node.isArray() ? new JacksonModelCollection((ArrayNode) node) : Collections.emptyList();
  }

  @Override
  public Optional<String> asText() {
    return node.isTextual() ? Optional.of(node.asText()) : Optional.empty();
  }

  @Override
  public Optional<OffsetDateTime> asDate() {
    return JsonUtils.toDate(node);
  }

  @Override
  public Optional<Number> asNumber() {
    return node.isNumber() ? Optional.of(node.asLong()) : Optional.empty();
  }

  @Override
  public <T> Optional<T> as(Class<T> clazz) {
    return clazz.isAssignableFrom(node.getClass())
        ? Optional.of(clazz.cast(node))
        : Optional.of(JsonUtils.convertValue(node, clazz));
  }

  @Override
  public String toString() {
    return node.toPrettyString();
  }

  @Override
  public Optional<Map<String, Object>> asMap() {
    // TODO use generic to avoid warning
    return node.isObject()
        ? Optional.of(JsonUtils.convertValue(node, Map.class))
        : Optional.empty();
  }

  @Override
  public Object asJavaObject() {
    return JsonUtils.toJavaValue(node);
  }

  @Override
  public Class<?> objectClass() {
    return node.getClass();
  }
}
