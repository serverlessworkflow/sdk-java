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
package io.serverlessworkflow.impl.persistence;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;

public class WorkflowPersistenceApplication<T extends WorkflowMinimumPersistenceReader>
    extends WorkflowApplication {

  private final T reader;
  private final WorkflowPersistenceWriter writer;
  private final boolean resumeAfterReboot;

  protected WorkflowPersistenceApplication(Builder<T> builder) {
    super(builder);
    this.reader = builder.reader;
    this.writer = builder.writer;
    this.resumeAfterReboot = builder.resumeAfterReboot;
  }

  public T persitenceReader() {
    return reader;
  }

  public void close() {
    super.close();
    try {
      reader.close();
    } catch (Exception e) {
    }
    try {
      writer.close();
    } catch (Exception e) {
    }
  }

  public static <T extends WorkflowMinimumPersistenceReader> Builder<T> builder(
      WorkflowPersistenceWriter writer, T reader) {
    return new Builder<>(writer, reader);
  }

  public static class Builder<T extends WorkflowMinimumPersistenceReader>
      extends io.serverlessworkflow.impl.WorkflowApplication.Builder {

    private final WorkflowPersistenceWriter writer;
    private final T reader;
    private boolean resumeAfterReboot = true;

    protected Builder(WorkflowPersistenceWriter writer, T reader) {
      this.writer = writer;
      this.reader = reader;
      super.withListener(new WorkflowPersistenceListener(writer));
    }

    public Builder<T> resumeAfterReboot(boolean resumeAfterReboot) {
      this.resumeAfterReboot = resumeAfterReboot;
      return this;
    }

    public WorkflowPersistenceApplication<T> build() {
      return new WorkflowPersistenceApplication<>(this);
    }
  }

  protected WorkflowDefinition createDefinition(Workflow workflow) {
    WorkflowDefinition definition = super.createDefinition(workflow);
    if (resumeAfterReboot) {
      reader.all(definition).forEach(WorkflowInstance::resume);
    }
    return definition;
  }
}
