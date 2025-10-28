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
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.serverlessworkflow.api.types.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class VolumesPropertySetter extends ContainerPropertySetter {

  VolumesPropertySetter(CreateContainerCmd createContainerCmd, Container configuration) {
    super(createContainerCmd, configuration);
  }

  @Override
  public void accept(StringExpressionResolver resolver) {
    if (configuration.getVolumes() != null
        && configuration.getVolumes().getAdditionalProperties() != null) {
      List<Bind> binds = new ArrayList<>();
      for (Map.Entry<String, Object> entry :
          configuration.getVolumes().getAdditionalProperties().entrySet()) {
        String hostPath = entry.getKey();
        if (entry.getValue() instanceof String containerPath) {
          String resolvedHostPath = resolver.resolve(hostPath);
          String resolvedContainerPath = resolver.resolve(containerPath);
          binds.add(new Bind(resolvedHostPath, new Volume(resolvedContainerPath)));
        } else {
          throw new IllegalArgumentException("Volume container paths must be strings");
        }
      }
      createContainerCmd.getHostConfig().withBinds(binds);
    }
  }
}
