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
package io.serverlessworkflow.validation.test;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.DelayState;
import io.serverlessworkflow.api.validation.ValidationError;
import io.serverlessworkflow.validation.WorkflowValidatorImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.serverlessworkflow.api.states.DefaultState.Type.DELAY;

public class WorkflowValidationTest {

    @Test
    public void testIncompleteJsonWithSchemaValidation() {
        WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
        List<ValidationError> validationErrors = workflowValidator.setSource("{\n" +
                "  \"id\": \"abc\" \n" +
                "}").validate();
        Assertions.assertNotNull(validationErrors);
        Assertions.assertEquals(4, validationErrors.size());
    }

    @Test
    public void testIncompleteYamlWithSchemaValidation() {
        WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
        List<ValidationError> validationErrors = workflowValidator.setSource("---\n" +
                "id: abc\n").validate();
        Assertions.assertNotNull(validationErrors);
        Assertions.assertEquals(4, validationErrors.size());
    }

    @Test
    public void testFromIncompleteWorkflow() {
        Workflow workflow = new Workflow().withId("test-workflow").withVersion("1.0")
                .withStates(Arrays.asList(
                        new DelayState().withName("delayState").withType(DELAY)
                                .withStart(
                                        new Start().withKind(Start.Kind.DEFAULT)
                                )
                                .withEnd(
                                        new End().withKind(End.Kind.DEFAULT)
                                )
                                .withTimeDelay("PT1M")
                        )
                );

        WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
        List<ValidationError> validationErrors = workflowValidator.setWorkflow(workflow).validate();
        Assertions.assertNotNull(validationErrors);
        Assertions.assertEquals(1, validationErrors.size());

        Assertions.assertEquals("Workflow name should not be empty", validationErrors.get(0).getMessage());
    }

    @Test
    public void testWorkflowMissingStates() {
        WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
        List<ValidationError> validationErrors = workflowValidator.setSource("{\n" +
                "\t\"id\": \"testwf\",\n" +
                "\t\"name\": \"test workflow\",\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"states\": []\n" +
                "}").validate();
        Assertions.assertNotNull(validationErrors);
        Assertions.assertEquals(1, validationErrors.size());

        Assertions.assertEquals("No states found", validationErrors.get(0).getMessage());
    }

    @Test
    public void testOperationStateNoFunctionRef() {
        WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
        List<ValidationError> validationErrors = workflowValidator.setSource("{\n" +
                "\"id\": \"checkInbox\",\n" +
                "  \"name\": \"Check Inbox Workflow\",\n" +
                "\"description\": \"Periodically Check Inbox\",\n" +
                "\"version\": \"1.0\",\n" +
                "\"functions\": [\n" +
                "\n" +
                "],\n" +
                "\"states\": [\n" +
                "    {\n" +
                "        \"name\": \"CheckInbox\",\n" +
                "        \"type\": \"operation\",\n" +
                "        \"start\": {\n" +
                "            \"kind\": \"scheduled\",\n" +
                "            \"schedule\": {\n" +
                "                \"cron\": \"0 0/15 * * * ?\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"actionMode\": \"sequential\",\n" +
                "        \"actions\": [\n" +
                "            {\n" +
                "                \"functionRef\": {\n" +
                "                    \"refName\": \"checkInboxFunction\"\n" +
                "                }\n" +
                "            }\n" +
                "        ],\n" +
                "        \"transition\": {\n" +
                "            \"nextState\": \"SendTextForHighPrioriry\"\n" +
                "        }\n" +
                "    },\n" +
                "    {\n" +
                "        \"name\": \"SendTextForHighPrioriry\",\n" +
                "        \"type\": \"foreach\",\n" +
                "        \"inputCollection\": \"{{ $.message }}\",\n" +
                "        \"iterationParam\": \"{{ $.singlemessage }}\",\n" +
                "        \"workflowId\": \"sendMessageWorkflowId\",\n" +
                "        \"end\": {\n" +
                "            \"kind\": \"default\"\n" +
                "        }\n" +
                "    }\n" +
                "]\n" +
                "}").validate();
        Assertions.assertNotNull(validationErrors);
        Assertions.assertEquals(1, validationErrors.size());

        Assertions.assertEquals("Operation State action functionRef does not reference an existing workflow function definition", validationErrors.get(0).getMessage());
    }
}
