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
package io.serverlessworkflow.api.types.utils;

import io.serverlessworkflow.api.types.SetTaskConfiguration;
import java.util.Map;

public class MapSetTaskConfiguration {

  private MapSetTaskConfiguration() {}

  public static SetTaskConfiguration map(Map<String, Object> map) {
    SetTaskConfiguration config = new SetTaskConfiguration();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      config.withAdditionalProperty(entry.getKey(), entry.getValue());
    }
    return config;
  }
}
