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

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.impl.WorkflowApplication;
import java.util.Collection;
import java.util.function.Consumer;

public interface EventConsumer<T extends EventRegistration, V extends EventRegistrationBuilder> {

  V listen(EventFilter filter, WorkflowApplication workflowApplication);

  Collection<V> listenToAll(WorkflowApplication workflowApplication);

  T register(V builder, Consumer<CloudEvent> consumer);

  void unregister(T register);
}
