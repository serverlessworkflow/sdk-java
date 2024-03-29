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
package io.serverlessworkflow.spi.test;

import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.spi.WorkflowPropertySourceProvider;
import io.serverlessworkflow.spi.WorkflowValidatorProvider;
import io.serverlessworkflow.spi.test.providers.TestWorkflowPropertySource;
import io.serverlessworkflow.spi.test.providers.TestWorkflowValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceProvidersTest {

  @Test
  public void testWorkflowValidatorProvider() {
    WorkflowValidator validator = WorkflowValidatorProvider.getInstance().get();
    Assertions.assertNotNull(validator);
    Assertions.assertTrue(validator instanceof TestWorkflowValidator);
  }

  @Test
  public void testWorkflowPropertySourceProvider() {
    WorkflowPropertySource propertySource = WorkflowPropertySourceProvider.getInstance().get();
    Assertions.assertNotNull(propertySource);
    Assertions.assertTrue(propertySource instanceof TestWorkflowPropertySource);
  }
}
