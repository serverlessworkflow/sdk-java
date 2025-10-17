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

import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.WorkflowContextData;

public interface PersistenceInstanceWriter extends AutoCloseable {

  void started(WorkflowContextData workflowContext);

  void completed(WorkflowContextData workflowContext);

  void failed(WorkflowContextData workflowContext, Throwable ex);

  void aborted(WorkflowContextData workflowContext);

  void suspended(WorkflowContextData workflowContext);

  void resumed(WorkflowContextData workflowContext);

  void taskRetried(WorkflowContextData workflowContext, TaskContextData taskContext);

  void taskStarted(WorkflowContextData workflowContext, TaskContextData taskContext);

  void taskCompleted(WorkflowContextData workflowContext, TaskContextData taskContext);

  @Override
  default void close() {}
}
