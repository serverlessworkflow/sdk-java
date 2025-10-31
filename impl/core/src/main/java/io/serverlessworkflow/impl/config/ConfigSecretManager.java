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
package io.serverlessworkflow.impl.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigSecretManager implements SecretManager {

  private final ConfigManager configManager;

  private Map<String, Map<String, String>> secretMap = new ConcurrentHashMap<>();

  public ConfigSecretManager(ConfigManager configManager) {
    this.configManager = configManager;
  }

  @Override
  public Map<String, String> secret(String secretName) {
    return secretMap.computeIfAbsent(secretName, this::buildMap);
  }

  private Map<String, String> buildMap(String secretName) {
    Map<String, String> map = new HashMap<String, String>();
    final String prefix = secretName + ".";
    for (String name : configManager.names()) {
      if (name.startsWith(prefix)) {
        configManager
            .config(name, String.class)
            .ifPresent(v -> map.put(name.substring(prefix.length()), v));
      }
    }
    return map;
  }
}
