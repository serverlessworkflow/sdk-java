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
package io.serverlessworkflow.impl.executors.func;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public class DataTypeConverterRegistry {

  private static final DataTypeConverterRegistry instance = new DataTypeConverterRegistry();

  public static DataTypeConverterRegistry get() {
    return instance;
  }

  @SuppressWarnings("rawtypes")
  private final Iterable<DataTypeConverter> converters;

  @SuppressWarnings("rawtypes")
  private final Map<Class<?>, Optional<DataTypeConverter>> convertersMap;

  private DataTypeConverterRegistry() {
    this.converters = ServiceLoader.load(DataTypeConverter.class);
    this.convertersMap = new ConcurrentHashMap<>();
  }

  @SuppressWarnings("rawtypes")
  public Optional<DataTypeConverter> find(Class clazz) {
    return convertersMap.computeIfAbsent(clazz, this::searchConverter);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Optional<DataTypeConverter> searchConverter(Class<?> clazz) {
    List<DataTypeConverter> candidates = new ArrayList<>();
    for (DataTypeConverter converter : converters) {
      if (converter.sourceType().equals(clazz)) {
        candidates.add(converter);
      }
    }
    if (!candidates.isEmpty()) {
      return first(candidates);
    }

    for (DataTypeConverter converter : converters) {
      if (converter.sourceType().isAssignableFrom(clazz)) {
        candidates.add(converter);
      }
    }
    return candidates.isEmpty() ? Optional.empty() : first(candidates);
  }

  @SuppressWarnings("rawtypes")
  private Optional<DataTypeConverter> first(List<DataTypeConverter> candidates) {
    Collections.sort(candidates);
    return Optional.of(candidates.get(0));
  }
}
