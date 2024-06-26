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

import static io.serverlessworkflow.api.WorkflowReader.readWorkflowFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.CallHTTP;
import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.Workflow;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ApiTest {

  @Test
  void testCallHTTPAPI() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/callHttp.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getAdditionalProperties()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getAdditionalProperties().values()).isNotEmpty();
    Task task = workflow.getDo().get(0).getAdditionalProperties().values().iterator().next();
    CallTask callTask = task.getCallTask();
    assertThat(callTask).isNotNull();
    assertThat(task.getDoTask()).isNull();
    CallHTTP httpCall = callTask.getCallHTTP();
    assertThat(httpCall).isNotNull();
    assertThat(callTask.getCallAsyncAPI()).isNull();
    assertThat(httpCall.getWith().getMethod()).isEqualTo("get");
  }
}
