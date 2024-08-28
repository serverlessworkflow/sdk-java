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

import io.serverlessworkflow.api.types.CallFunction;
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
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    if (task.get() instanceof CallTask) {
      CallTask callTask = task.getCallTask();
      assertThat(callTask).isNotNull();
      assertThat(task.getDoTask()).isNull();
      CallHTTP httpCall = callTask.getCallHTTP();
      assertThat(httpCall).isNotNull();
      assertThat(callTask.getCallAsyncAPI()).isNull();
      assertThat(httpCall.getWith().getMethod()).isEqualTo("get");
    }
  }

  @Test
  void testCallFunctionAPIWithoutArguments() throws IOException {
    Workflow workflow = readWorkflowFromClasspath("features/callFunction.yaml");
    assertThat(workflow.getDo()).isNotEmpty();
    assertThat(workflow.getDo().get(0).getName()).isNotNull();
    assertThat(workflow.getDo().get(0).getTask()).isNotNull();
    Task task = workflow.getDo().get(0).getTask();
    CallTask callTask = task.getCallTask();
    assertThat(callTask).isNotNull();
    assertThat(callTask.get()).isInstanceOf(CallFunction.class);
    if (callTask.get() instanceof CallFunction) {
      CallFunction functionCall = callTask.getCallFunction();
      assertThat(functionCall).isNotNull();
      assertThat(callTask.getCallAsyncAPI()).isNull();
      assertThat(functionCall.getWith()).isNull();
    }
  }
}
