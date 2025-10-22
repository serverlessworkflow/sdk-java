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

import io.serverlessworkflow.impl.AbstractWorkflowModel;
import io.serverlessworkflow.impl.WorkflowModel;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaModel extends AbstractWorkflowModel {

  protected Object object;

  public JavaModel(Object object) {
    this.object = asJavaObject(object);
  }

  protected void setObject(Object object) {
    this.object = object;
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
  public Class<?> objectClass() {
    return object != null ? object.getClass() : Object.class;
  }

  @Override
  protected <T> Optional<T> convert(Class<T> clazz) {
    return object != null && clazz.isAssignableFrom(object.getClass())
        ? Optional.of(clazz.cast(object))
        : Optional.empty();
  }
}
