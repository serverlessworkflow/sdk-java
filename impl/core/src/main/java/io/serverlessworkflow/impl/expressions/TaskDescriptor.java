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
package io.serverlessworkflow.impl.expressions;

import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowModel;

public record TaskDescriptor(
    String name,
    String reference,
    TaskBase definition,
    WorkflowModel rawInput,
    WorkflowModel rawOutput,
    DateTimeDescriptor startedAt) {

  public static TaskDescriptor of(TaskContext context) {
    return new TaskDescriptor(
        context.taskName(),
        context.position().jsonPointer(),
        context.task(),
        context.rawInput(),
        context.rawOutput(),
        DateTimeDescriptor.from(context.startedAt()));
  }
}
