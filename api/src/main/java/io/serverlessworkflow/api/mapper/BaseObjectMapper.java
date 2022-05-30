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
package io.serverlessworkflow.api.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.util.Map;

public class BaseObjectMapper extends ObjectMapper {

  private WorkflowModule workflowModule;

  public BaseObjectMapper(JsonFactory factory, WorkflowPropertySource workflowPropertySource) {
    super(factory);

    workflowModule = new WorkflowModule(workflowPropertySource);

    configure(SerializationFeature.INDENT_OUTPUT, true);
    registerModule(workflowModule);
    configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
    configOverride(Map.class)
        .setInclude(
            JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
  }

  public WorkflowModule getWorkflowModule() {
    return workflowModule;
  }
}
