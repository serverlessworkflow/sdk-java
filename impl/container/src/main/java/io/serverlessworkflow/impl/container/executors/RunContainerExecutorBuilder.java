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
package io.serverlessworkflow.impl.container.executors;

import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.api.types.ContainerLifetime;
import io.serverlessworkflow.api.types.ContainerLifetime.ContainerCleanupPolicy;
import io.serverlessworkflow.api.types.RunContainer;
import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.api.types.TimeoutAfter;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import io.serverlessworkflow.impl.executors.CallableTask;
import io.serverlessworkflow.impl.executors.RunnableTaskBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class RunContainerExecutorBuilder implements RunnableTaskBuilder<RunContainer> {

  @Override
  public CallableTask build(RunContainer taskConfiguration, WorkflowDefinition definition) {
    Collection<ContainerPropertySetter> propertySetters = new ArrayList<>();
    Container container = taskConfiguration.getContainer();
    propertySetters.add(new NamePropertySetter(definition, container));
    propertySetters.add(new CommandPropertySetter(definition, container));
    propertySetters.add(new ContainerEnvironmentPropertySetter(definition, container));
    propertySetters.add(new LifetimePropertySetter(container));
    propertySetters.add(new PortsPropertySetter(container));
    propertySetters.add(new VolumesPropertySetter(definition, container));

    ContainerCleanupPolicy policy = null;
    WorkflowValueResolver<Duration> timeout = null;
    ContainerLifetime lifetime = container.getLifetime();
    if (lifetime != null) {
      policy = lifetime.getCleanup();
      TimeoutAfter afterTimeout = lifetime.getAfter();
      if (afterTimeout != null)
        timeout = WorkflowUtils.fromTimeoutAfter(definition.application(), afterTimeout);
    }
    return new ContainerRunner(
        propertySetters, Optional.ofNullable(timeout), policy, container.getImage());
  }

  @Override
  public boolean accept(Class<? extends RunTaskConfiguration> clazz) {
    return RunContainer.class.equals(clazz);
  }
}
