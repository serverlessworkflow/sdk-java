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
package io.serverlessworkflow.impl.test;

import io.serverlessworkflow.api.WorkflowReader;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import java.io.IOException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenAPIWorkflowDefinitionTest {

  private static WorkflowApplication app;

  @BeforeAll
  static void setUp() {
    app = WorkflowApplication.builder().build();
  }

  @Test
  void testOpenAPIWorkflowExecution() throws IOException {

    WorkflowModel model =
        app.workflowDefinition(
                WorkflowReader.readWorkflowFromClasspath(
                    "workflows-samples/openapi/findPetsByStatus.yaml"))
            .instance(List.of())
            .start()
            .join();

    Assertions.assertThat(model.asCollection()).isNotEmpty();
  }
}
