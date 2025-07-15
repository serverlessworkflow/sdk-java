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
package io.serverlessworkflow.impl.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.jackson.JsonCloudEventData;
import io.serverlessworkflow.impl.json.JsonUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloudEventUtils {

  public static JsonNode toJsonNode(CloudEvent event) {
    ObjectNode result = JsonUtils.mapper().createObjectNode();
    if (event.getData() != null) {
      result.set("data", toJsonNode(event.getData()));
    }
    if (event.getSubject() != null) {
      result.put("subject", event.getSubject());
    }
    if (event.getDataContentType() != null) {
      result.put("datacontenttype", event.getDataContentType());
    }
    result.put("id", event.getId());
    result.put("source", event.getSource().toString());
    result.put("type", event.getType());
    result.put("specversion", event.getSpecVersion().toString());
    if (event.getDataSchema() != null) {
      result.put("dataschema", event.getDataSchema().toString());
    }
    if (event.getTime() != null) {
      result.put("time", event.getTime().toString());
    }
    event
        .getExtensionNames()
        .forEach(n -> result.set(n, JsonUtils.fromValue(event.getExtension(n))));
    return result;
  }

  public static OffsetDateTime toOffset(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC);
  }

  public static JsonNode toJsonNode(CloudEventData data) {
    if (data == null) {
      return NullNode.instance;
    }
    try {
      return data instanceof JsonCloudEventData
          ? ((JsonCloudEventData) data).getNode()
          : JsonUtils.mapper().readTree(data.toBytes());
    } catch (IOException io) {
      throw new UncheckedIOException(io);
    }
  }

  public static Map<String, Object> extensions(CloudEvent event) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (String name : event.getExtensionNames()) {
      result.put(name, event.getExtension(name));
    }
    return result;
  }

  private CloudEventUtils() {}
}
