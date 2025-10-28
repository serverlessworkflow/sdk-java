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

import static io.serverlessworkflow.api.types.ContainerLifetime.*;

import com.github.dockerjava.api.command.CreateContainerCmd;
import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.api.types.ContainerLifetime;

class LifetimePropertySetter extends ContainerPropertySetter {

  LifetimePropertySetter(CreateContainerCmd createContainerCmd, Container configuration) {
    super(createContainerCmd, configuration);
  }

  @Override
  public void accept(StringExpressionResolver resolver) {
    // case of cleanup=eventually processed at ContainerRunner
    if (configuration.getLifetime() != null) {
      ContainerLifetime lifetime = configuration.getLifetime();
      ContainerCleanupPolicy cleanupPolicy = lifetime.getCleanup();
      if (cleanupPolicy.equals(ContainerCleanupPolicy.ALWAYS)) {
        createContainerCmd.getHostConfig().withAutoRemove(true);
      } else if (cleanupPolicy.equals(ContainerCleanupPolicy.NEVER)) {
        createContainerCmd.getHostConfig().withAutoRemove(false);
      }
    }
  }
}
