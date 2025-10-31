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
package io.serverlessworkflow.impl;

import io.serverlessworkflow.types.Errors;

public record WorkflowError(
    String type, int status, String instance, String title, String details) {

  public static Builder error(String type, int status) {
    return new Builder(type, status);
  }

  public static Builder authorization() {
    return error(Errors.AUTHORIZATION.toString(), Errors.AUTHORIZATION.status());
  }

  public static Builder communication(int status, TaskContext context, Exception ex) {
    return communication(status, context, ex.getMessage());
  }

  public static Builder communication(int status, TaskContext context, String title) {
    return new Builder(Errors.COMMUNICATION.toString(), status)
        .instance(context.position().jsonPointer())
        .title(title);
  }

  public static Builder communication(TaskContext context, String title) {
    return communication(Errors.COMMUNICATION.status(), context, title);
  }

  public static Builder runtime(int status, TaskContext context, Exception ex) {
    return new Builder(Errors.RUNTIME.toString(), status)
        .instance(context.position().jsonPointer())
        .title(ex.getMessage());
  }

  public static Builder runtime(TaskContext context, Exception ex) {
    return runtime(Errors.RUNTIME.status(), context, ex);
  }

  public static class Builder {

    private final String type;
    private int status;
    private String instance;
    private String title;
    private String details;

    private Builder(String type, int status) {
      this.type = type;
      this.status = status;
    }

    public Builder instance(String instance) {
      this.instance = instance;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder details(String details) {
      this.details = details;
      return this;
    }

    public WorkflowError build() {
      return new WorkflowError(type, status, instance, title, details);
    }
  }
}
