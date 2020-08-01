/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.serverlessworkflow.spi.test.providers;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.validation.ValidationError;

import java.util.List;

public class TestWorkflowValidator implements WorkflowValidator {

    @Override
    public WorkflowValidator setWorkflow(Workflow workflow) {
        return this;
    }

    @Override
    public WorkflowValidator setSource(String source) {
        return this;
    }

    @Override
    public List<ValidationError> validate() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public WorkflowValidator setSchemaValidationEnabled(boolean schemaValidationEnabled) {
        return this;
    }

    @Override
    public WorkflowValidator reset() {
        return this;
    }
}
