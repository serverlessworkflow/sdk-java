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
package io.serverlessworkflow.impl.persistence.bigmap;

import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowApplication.Builder;
import io.serverlessworkflow.impl.marshaller.DefaultBufferFactory;
import io.serverlessworkflow.impl.marshaller.WorkflowBufferFactory;
import io.serverlessworkflow.impl.persistence.WorkflowPersistenceListener;

public class BytesBigMapApplicationBuilder {

  public static BytesBigMapApplicationBuilder builder(
      WorkflowApplication.Builder builder,
      BigMapPersistenceStore<String, byte[], byte[], byte[]> store) {
    return new BytesBigMapApplicationBuilder(builder, store);
  }

  private final BigMapPersistenceStore<String, byte[], byte[], byte[]> store;
  private final WorkflowApplication.Builder appBuilder;
  private WorkflowBufferFactory factory;

  protected BytesBigMapApplicationBuilder(
      Builder appBuilder, BigMapPersistenceStore<String, byte[], byte[], byte[]> store) {
    this.appBuilder = appBuilder;
    this.store = store;
  }

  public BytesBigMapApplicationBuilder withFactory(WorkflowBufferFactory factory) {
    this.factory = factory;
    return this;
  }

  public WorkflowApplication build() {
    if (factory == null) {
      factory = DefaultBufferFactory.factory();
    }
    appBuilder.withListener(
        new WorkflowPersistenceListener(new BytesBigMapPersistenceWriter(store, factory)));
    return appBuilder.build();
  }
}
