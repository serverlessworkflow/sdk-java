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
package io.serverlessworkflow.api.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.test.utils.WorkflowTestUtils;
import org.junit.jupiter.api.Test;

class CodegenTest {

  @Test
  void collectionsShouldNotBeInitializedByDefault() {
    Workflow workflow =
        Workflow.fromSource(WorkflowTestUtils.readWorkflowFile("/features/functionrefs.json"));
    assertThat(workflow.getAnnotations()).isNull();
  }
}
