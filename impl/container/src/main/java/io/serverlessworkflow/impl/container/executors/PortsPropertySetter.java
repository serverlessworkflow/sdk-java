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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class PortsPropertySetter extends ContainerPropertySetter {

  PortsPropertySetter(CreateContainerCmd createContainerCmd, Container configuration) {
    super(createContainerCmd, configuration);
  }

  @Override
  public void accept(Function<String, String> resolver) {
    if (configuration.getPorts() != null
        && configuration.getPorts().getAdditionalProperties() != null) {
      Ports portBindings = new Ports();
      List<ExposedPort> exposed = new ArrayList<>();

      for (Map.Entry<String, Object> entry :
          configuration.getPorts().getAdditionalProperties().entrySet()) {
        int hostPort = Integer.parseInt(entry.getKey());
        int containerPort = Integer.parseInt(entry.getValue().toString());
        ExposedPort exposedPort = ExposedPort.tcp(containerPort);
        portBindings.bind(exposedPort, Ports.Binding.bindPort(hostPort));
        exposed.add(exposedPort);
      }
      createContainerCmd.withExposedPorts(exposed.toArray(new ExposedPort[0]));
      createContainerCmd.getHostConfig().withPortBindings(portBindings);
    }
  }
}
