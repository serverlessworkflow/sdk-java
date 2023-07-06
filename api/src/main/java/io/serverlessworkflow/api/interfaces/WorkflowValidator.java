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
package io.serverlessworkflow.api.interfaces;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.validation.ValidationError;
import java.util.Collection;

public interface WorkflowValidator {

  WorkflowValidator setWorkflow(Workflow workflow);

  WorkflowValidator setSource(String source);

  Collection<ValidationError> validate();

  boolean isValid();

  WorkflowValidator setSchemaValidationEnabled(boolean schemaValidationEnabled);

  WorkflowValidator reset();
}
