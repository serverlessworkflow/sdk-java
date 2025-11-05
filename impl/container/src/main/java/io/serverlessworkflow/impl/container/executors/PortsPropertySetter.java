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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import io.serverlessworkflow.api.types.Container;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PortsPropertySetter implements ContainerPropertySetter {

  private Ports portBindings = new Ports();
  private List<ExposedPort> exposed = new ArrayList<>();

  PortsPropertySetter(Container configuration) {
    if (configuration.getPorts() != null
        && configuration.getPorts().getAdditionalProperties() != null) {
      for (Map.Entry<String, Object> entry :
          configuration.getPorts().getAdditionalProperties().entrySet()) {
        ExposedPort exposedPort = ExposedPort.tcp(Integer.parseInt(entry.getKey()));
        exposed.add(exposedPort);
        portBindings.bind(exposedPort, Ports.Binding.bindPort(from(entry.getValue())));
      }
    }
  }

  @Override
  public void accept(
      CreateContainerCmd command,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel model) {
    command.withExposedPorts(exposed);
    command.getHostConfig().withPortBindings(portBindings);
  }

  private static Integer from(Object obj) {
    if (obj instanceof Integer number) {
      return number;
    } else if (obj instanceof Number number) {
      return number.intValue();
    } else if (obj instanceof String str) {
      return Integer.parseInt(str);
    } else if (obj != null) {
      return Integer.parseInt(obj.toString());
    } else {
      throw new IllegalArgumentException("Null value for port key");
    }
  }
}
