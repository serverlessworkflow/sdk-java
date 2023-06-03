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
package io.serverlessworkflow.validation.test;

import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.validation.ValidationError;
import io.serverlessworkflow.validation.WorkflowValidatorImpl;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkflowValidationSchemaTest {

  @Test
  public void testIncompleteJsonWithSchemaValidation() {
    WorkflowValidator workflowValidator =
            new WorkflowValidatorImpl();
    List<ValidationError> validationErrors =
            workflowValidator.setSource("{\"id\": \"abc\"}").validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());
    Assertions.assertTrue(
            validationErrors.get(0).getMessage().indexOf("required key [specVersion] not found") > 0);
  }

  @Test
  public void testSchemaWorkflowMissingStates() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    List<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "  \"version\": \"1.0\",\n"
                    + "  \"specVersion\": \"0.8\",\n"
                    + "  \"name\": \"Hello World Workflow\",\n"
                    + "  \"description\": \"Inject Hello World\",\n"
                    + "  \"start\": \"Hello State\",\n"
                    + "  \"states\": [\n"
                    + "    {\n"
                    + "      \"name\": \"Hello State\",\n"
                    + "      \"type\": \"inject\",\n"
                    + "      \"data\": {\n"
                    + "        \"result\": \"Hello World!\"\n"
                    + "      },\n"
                    + "      \"end\": true\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}")
            .validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());
    Assertions.assertTrue(
            validationErrors.get(0).getMessage().indexOf("required key [id] not found") > 0);
    Assertions.assertTrue(
            validationErrors.get(0).getMessage().indexOf("required key [key] not found") > 0);
  }
}
