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
package io.serverlessworkflow.fluent.func.serialization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.serverlessworkflow.api.types.func.FilterSerializable;
import io.serverlessworkflow.api.types.func.FunctionObject;
import io.serverlessworkflow.api.types.utils.ReflectionUtils;
import io.serverlessworkflow.serialization.DeserializeHelper;
import java.io.IOException;
import java.lang.invoke.SerializedLambda;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

class SerializationUtils {

  private SerializationUtils() {}

  private static final String TYPE = "type";
  private static final String VALUE = "value";
  private static final String NULL = "null";

  public static void serializeObjectWithType(JsonGenerator gen, Object value) throws IOException {
    gen.writeStartObject();
    if (value == null) {
      gen.writeStringField(TYPE, NULL);
    } else {
      if (value instanceof FunctionObject) {
        gen.writeStringField(TYPE, SerializedLambda.class.getName());
        try {
          gen.writeObjectField(VALUE, ReflectionUtils.serializedLambda(value));
        } catch (ReflectiveOperationException e) {
          throw new IOException(e);
        }
      } else {
        gen.writeStringField(TYPE, value.getClass().getName());
        if (value instanceof Optional optional) {
          writeOptionalWithType(gen, optional);
        } else {
          gen.writeObjectField(VALUE, value);
        }
      }
    }
    gen.writeEndObject();
  }

  public static Object deserializeObjectWithType(DeserializationContext ctxt, JsonNode objectNode)
      throws IOException, ReflectiveOperationException {
    String className = objectNode.get(TYPE).asText();
    if (NULL.equals(className)) {
      return null;
    }
    Class<?> clazz = ReflectionUtils.loadClass(className);
    if (clazz.equals(Optional.class)) {
      return readOptionalWithType(ctxt, objectNode.get(VALUE));
    } else {
      Object value = ctxt.readTreeAsValue(objectNode.get(VALUE), clazz);
      return value instanceof SerializedLambda sl
          ? ReflectionUtils.functionFromSerialized(sl)
          : value;
    }
  }

  public static void writeOptionalWithType(JsonGenerator gen, Optional<?> optional)
      throws IOException {
    if (!optional.isEmpty()) {
      gen.writeFieldName(VALUE);
      serializeObjectWithType(gen, optional.orElseThrow());
    }
  }

  public static Optional<?> readOptionalWithType(DeserializationContext ctxt, JsonNode objectNode)
      throws IOException, ReflectiveOperationException {
    return objectNode == null
        ? Optional.empty()
        : Optional.of(deserializeObjectWithType(ctxt, objectNode));
  }

  public static void serializeMap(JsonGenerator gen, Map<String, Object> map) throws IOException {
    gen.writeStartObject();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      gen.writeFieldName(entry.getKey());
      SerializationUtils.serializeObjectWithType(gen, entry.getValue());
    }
    gen.writeEndObject();
  }

  public static void deserializeMap(
      JsonParser p, DeserializationContext ctxt, Map<String, Object> map)
      throws IOException, ReflectiveOperationException {
    ObjectNode node = (ObjectNode) ctxt.readTree(p);
    for (Entry<String, JsonNode> item : node.properties()) {
      map.put(item.getKey(), deserializeObjectWithType(ctxt, item.getValue()));
    }
  }

  public static <T> T deserializeFilterClass(
      JsonParser p,
      DeserializationContext ctxt,
      Function<FilterSerializable, T> setter,
      Class<T> objectClass)
      throws IOException {
    TreeNode treeNode = p.readValueAsTree();
    if (treeNode instanceof ObjectNode node && SerializationUtils.hasType(node)) {
      try {
        return setter.apply(
            (FilterSerializable) SerializationUtils.deserializeObjectWithType(ctxt, node));
      } catch (ReflectiveOperationException e) {
        throw new IOException(e);
      }
    } else {
      return DeserializeHelper.deserializeOneOf(
          treeNode, p, objectClass, List.of(String.class, Object.class));
    }
  }

  public static boolean isFilterSerializable(Object object) {
    return object instanceof FilterSerializable;
  }

  public static boolean hasType(ObjectNode node) {
    return node.has(TYPE);
  }
}
