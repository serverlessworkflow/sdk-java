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
package io.serverlessworkflow.impl.lifecycle.ce;

import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowFailedEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

public record WorkflowErrorCEData(
    String type, Integer status, String instance, String title, String detail) {

  public static WorkflowErrorCEData error(TaskFailedEvent ev) {
    return error(ev.cause());
  }

  public static WorkflowErrorCEData error(WorkflowFailedEvent ev) {
    return error(ev.cause());
  }

  private static WorkflowErrorCEData error(Throwable cause) {
    return cause instanceof WorkflowException ex ? error(ex) : commonError(cause);
  }

  private static WorkflowErrorCEData commonError(Throwable cause) {
    StringWriter stackTrace = new StringWriter();
    try (PrintWriter writer = new PrintWriter(stackTrace)) {
      cause.printStackTrace(writer);
      return new WorkflowErrorCEData(
          cause.getClass().getTypeName(), null, stackTrace.toString(), null, cause.getMessage());
    }
  }

  private static WorkflowErrorCEData error(WorkflowException ex) {
    WorkflowError error = ex.getWorflowError();
    return new WorkflowErrorCEData(
        error.type(), error.status(), error.instance(), error.title(), error.details());
  }
}
