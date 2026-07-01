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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GlobalConverterRegistry {

  private static GlobalConverterRegistry instance = new GlobalConverterRegistry();
  private Map<String, Function<?, ?>> converterMap;

  private GlobalConverterRegistry() {
    this.converterMap = new HashMap<>();
  }

  public static GlobalConverterRegistry get() {
    return instance;
  }

  public <T, V> GlobalConverterRegistry registerConverter(String key, Function<T, V> converter) {
    converterMap.put(key, converter);
    return this;
  }

  public Optional<Function> findConverter(String key) {
    return Optional.ofNullable(converterMap.get(key));
  }
}
