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
package io.serverlessworkflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

public enum WorkflowFormat {
  JSON(ObjectMapperFactory.jsonMapper()),
  YAML(ObjectMapperFactory.yamlMapper());

  private final ObjectMapper mapper;

  public static WorkflowFormat fromPath(Path path) {
    return fromFileName(path.getFileName().toString());
  }

  public static WorkflowFormat fromFileName(String fileName) {
    return fileName.endsWith(".json") ? JSON : YAML;
  }

  private WorkflowFormat(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ObjectMapper mapper() {
    return mapper;
  }
}
