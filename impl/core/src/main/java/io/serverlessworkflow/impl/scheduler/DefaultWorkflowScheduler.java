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
import io.serverlessworkflow.impl.WorkflowScheduler;
import io.serverlessworkflow.impl.events.EventRegistrationBuilderInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DefaultWorkflowScheduler implements WorkflowScheduler {

  private Map<WorkflowDefinition, Collection<WorkflowInstance>> instances =
      new ConcurrentHashMap<>();

  @Override
  public Collection<WorkflowInstance> scheduledInstances(WorkflowDefinition definition) {
    return Collections.unmodifiableCollection(theInstances(definition));
  }

  @Override
  public ScheduledEventConsumer eventConsumer(
      WorkflowDefinition definition,
      Function<CloudEvent, WorkflowModel> converter,
      EventRegistrationBuilderInfo builderInfo) {
    return new ScheduledEventConsumer(definition, converter, builderInfo) {
      @Override
      protected void addScheduledInstance(WorkflowInstance instance) {
        theInstances(definition).add(instance);
      }
    };
  }

  private Collection<WorkflowInstance> theInstances(WorkflowDefinition definition) {
    return instances.computeIfAbsent(definition, def -> new ArrayList<>());
  }
}
