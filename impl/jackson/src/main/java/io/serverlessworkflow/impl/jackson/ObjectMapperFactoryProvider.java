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
package io.serverlessworkflow.impl.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public class ObjectMapperFactoryProvider implements Supplier<ObjectMapperFactory> {

  private static ObjectMapperFactoryProvider instance = new ObjectMapperFactoryProvider();

  public static ObjectMapperFactoryProvider instance() {
    return instance;
  }

  private ObjectMapperFactory objectMapperFactory;

  private ObjectMapperFactoryProvider() {}

  public void setFactory(ObjectMapperFactory objectMapperFactory) {
    this.objectMapperFactory = Objects.requireNonNull(objectMapperFactory);
  }

  @Override
  public ObjectMapperFactory get() {
    return objectMapperFactory != null
        ? objectMapperFactory
        : ServiceLoader.load(ObjectMapperFactory.class)
            .findFirst()
            .orElseGet(() -> () -> new ObjectMapper().findAndRegisterModules());
  }
}
