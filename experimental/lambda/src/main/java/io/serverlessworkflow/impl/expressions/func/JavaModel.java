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
package io.serverlessworkflow.impl.expressions.func;

import io.cloudevents.CloudEventData;
import io.serverlessworkflow.impl.WorkflowModel;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

class JavaModel implements WorkflowModel {

  private Object object;

  static final JavaModel TrueModel = new JavaModel(Boolean.TRUE);
  static final JavaModel FalseModel = new JavaModel(Boolean.FALSE);
  static final JavaModel NullModel = new JavaModel(null);

  JavaModel(Object object) {
    this.object = asJavaObject(object);
  }

  @Override
  public void forEach(BiConsumer<String, WorkflowModel> consumer) {
    asMap()
        .ifPresent(
            m ->
                m.forEach(
                    (k, v) ->
                        consumer.accept(
                            k, v instanceof WorkflowModel model ? model : new JavaModel(v))));
  }

  @Override
  public Optional<Boolean> asBoolean() {
    return object instanceof Boolean value ? Optional.of(value) : Optional.empty();
  }

  @Override
  public Collection<WorkflowModel> asCollection() {
    return object instanceof Collection value
        ? new JavaModelCollection(value)
        : Collections.emptyList();
  }

  @Override
  public Optional<String> asText() {
    return object instanceof String value ? Optional.of(value) : Optional.empty();
  }

  @Override
  public Optional<OffsetDateTime> asDate() {
    return object instanceof OffsetDateTime value ? Optional.of(value) : Optional.empty();
  }

  @Override
  public Optional<Number> asNumber() {
    return object instanceof Number value ? Optional.of(value) : Optional.empty();
  }

  @Override
  public Optional<CloudEventData> asCloudEventData() {
    return object instanceof CloudEventData value ? Optional.of(value) : Optional.empty();
  }

  @Override
  public Optional<Map<String, Object>> asMap() {
    return object instanceof Map ? Optional.of((Map<String, Object>) object) : Optional.empty();
  }

  @Override
  public Object asJavaObject() {
    return object;
  }

  static Object asJavaObject(Object object) {
    if (object instanceof WorkflowModel model) {
      return model.asJavaObject();
    } else if (object instanceof Map map) {
      return ((Map<String, Object>) map)
          .entrySet().stream()
              .collect(Collectors.toMap(Entry::getKey, e -> asJavaObject(e.getValue())));
    } else if (object instanceof Collection col) {
      return col.stream().map(JavaModel::asJavaObject).collect(Collectors.toList());
    } else {
      return object;
    }
  }

  @Override
  public Object asIs() {
    return object;
  }

  @Override
  public Class<?> objectClass() {
    return object != null ? object.getClass() : Object.class;
  }

  @Override
  public <T> Optional<T> as(Class<T> clazz) {
    return object != null && object.getClass().isAssignableFrom(clazz)
        ? Optional.of(clazz.cast(object))
        : Optional.empty();
  }
}
