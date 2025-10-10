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
package io.serverlessworkflow.impl.scheduler;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelCollection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class ScheduledEventConsumer {

  private final Function<CloudEvent, WorkflowModel> converter;
  private final WorkflowDefinition definition;

  protected ScheduledEventConsumer(
      WorkflowDefinition definition, Function<CloudEvent, WorkflowModel> converter) {
    this.definition = definition;
    this.converter = converter;
  }

  public void accept(
      CloudEvent t, CompletableFuture<WorkflowModel> u, WorkflowModelCollection col) {
    WorkflowModel model = converter.apply(t);
    col.add(model);
    u.complete(model);
  }

  public void start(Object model) {
    WorkflowInstance instance = definition.instance(model);
    addScheduledInstance(instance);
    instance.start();
  }

  protected abstract void addScheduledInstance(WorkflowInstance instace);
}
