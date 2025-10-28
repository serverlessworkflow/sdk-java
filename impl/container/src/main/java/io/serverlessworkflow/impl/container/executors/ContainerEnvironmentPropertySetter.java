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
import io.serverlessworkflow.api.types.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ContainerEnvironmentPropertySetter extends ContainerPropertySetter {

  ContainerEnvironmentPropertySetter(
      CreateContainerCmd createContainerCmd, Container configuration) {
    super(createContainerCmd, configuration);
  }

  @Override
  public void accept(StringExpressionResolver resolver) {
    if (!(configuration.getEnvironment() == null
        || configuration.getEnvironment().getAdditionalProperties() == null)) {
      List<String> envs = new ArrayList<>();
      for (Map.Entry<String, Object> entry :
          configuration.getEnvironment().getAdditionalProperties().entrySet()) {
        String key = entry.getKey();
        if (entry.getValue() instanceof String value) {
          String resolvedValue = resolver.resolve(value);
          envs.add(key + "=" + resolvedValue);
        } else {
          throw new IllegalArgumentException("Environment variable values must be strings");
        }
      }
      createContainerCmd.withEnv(envs.toArray(new String[0]));
    }
  }
}
