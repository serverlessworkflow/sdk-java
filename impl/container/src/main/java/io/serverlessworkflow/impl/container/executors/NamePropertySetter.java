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

import static io.serverlessworkflow.impl.WorkflowUtils.isValid;

import com.github.dockerjava.api.command.CreateContainerCmd;
import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.util.Optional;

class NamePropertySetter implements ContainerPropertySetter {

  private final Optional<WorkflowValueResolver<String>> containerName;

  NamePropertySetter(WorkflowDefinition definition, Container container) {
    String containerName = container.getName();
    this.containerName =
        isValid(containerName)
            ? Optional.of(WorkflowUtils.buildStringFilter(definition.application(), containerName))
            : Optional.empty();
  }

  @Override
  public void accept(
      CreateContainerCmd createContainerCmd,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel model) {
    containerName
        .map(c -> c.apply(workflowContext, taskContext, model))
        .ifPresent(createContainerCmd::withName);
  }
}
