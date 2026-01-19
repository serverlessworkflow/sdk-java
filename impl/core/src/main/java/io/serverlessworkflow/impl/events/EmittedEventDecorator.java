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
package io.serverlessworkflow.impl.events;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.serverlessworkflow.impl.ServicePriority;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;

/**
 * Interface for decorating {@link CloudEventBuilder} objects.
 *
 * <p>Implementations should be loaded via ServiceLoader and are sorted by priority in ascending
 * order (lower priority numbers executed first). Decorators are applied in sequence, where later
 * decorators can override headers set by earlier decorators with the same header name.
 *
 * @see ServicePriority
 */
public interface EmittedEventDecorator extends ServicePriority {

  void decorate(
      CloudEventBuilder ceBuilder, WorkflowContext workflowContext, TaskContext taskContext);
}
