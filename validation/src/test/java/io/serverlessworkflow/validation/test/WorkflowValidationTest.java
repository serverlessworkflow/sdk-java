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

import static io.serverlessworkflow.api.states.DefaultState.Type.SLEEP;

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.interfaces.WorkflowValidator;
import io.serverlessworkflow.api.start.Start;
import io.serverlessworkflow.api.states.SleepState;
import io.serverlessworkflow.api.validation.ValidationError;
import io.serverlessworkflow.validation.WorkflowValidatorImpl;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkflowValidationTest {

  @Test
  public void testIncompleteJsonWithSchemaValidation() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator.setSource("{\n" + "  \"id\": \"abc\" \n" + "}").validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(3, validationErrors.size());
  }

  @Test
  public void testIncompleteYamlWithSchemaValidation() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator.setSource("---\n" + "key: abc\n").validate();
    Assertions.assertNotNull(validationErrors);
    System.out.println(validationErrors);
    Assertions.assertEquals(3, validationErrors.size());
  }

  @Test
  public void testFromIncompleteWorkflow() {
    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withVersion("1.0")
            .withStart(new Start())
            .withStates(
                Arrays.asList(
                    new SleepState()
                        .withName("sleepState")
                        .withType(SLEEP)
                        .withEnd(new End())
                        .withDuration("PT1M")));

    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator.setWorkflow(workflow).validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());
    Assertions.assertTrue(
        validationErrors.stream()
            .anyMatch(
                v ->
                    v.getMessage()
                        .equals("No state name found that matches the workflow start definition")));
  }

  @Test
  public void testWorkflowMissingStates() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "\t\"id\": \"testwf\",\n"
                    + "\t\"name\": \"test workflow\",\n"
                    + "  \"version\": \"1.0\",\n"
                    + "  \"start\": \"SomeState\",\n"
                    + "  \"states\": []\n"
                    + "}")
            .validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());

    Assertions.assertTrue(
        validationErrors.stream().anyMatch(v -> v.getMessage().equals("No states found")));
  }

  @Test
  public void testWorkflowMissingStatesIdAndKey() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "\t\"name\": \"test workflow\",\n"
                    + "  \"version\": \"1.0\",\n"
                    + "  \"start\": \"SomeState\",\n"
                    + "  \"states\": []\n"
                    + "}")
            .validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(2, validationErrors.size());
    Assertions.assertEquals(
        validationErrors.stream()
            .filter(
                v ->
                    v.getMessage().equals("No states found")
                        || v.getMessage().equals("Workflow id or key should not be empty"))
            .count(),
        2);
  }

  @Test
  public void testOperationStateNoFunctionRef() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "\"id\": \"checkInbox\",\n"
                    + "\"name\": \"Check Inbox Workflow\",\n"
                    + "\"description\": \"Periodically Check Inbox\",\n"
                    + "\"version\": \"1.0\",\n"
                    + "\"start\": \"CheckInbox\",\n"
                    + "\"functions\": [\n"
                    + "\n"
                    + "],\n"
                    + "\"states\": [\n"
                    + "    {\n"
                    + "        \"name\": \"CheckInbox\",\n"
                    + "        \"type\": \"operation\",\n"
                    + "        \"actionMode\": \"sequential\",\n"
                    + "        \"actions\": [\n"
                    + "            {\n"
                    + "                \"functionRef\": {\n"
                    + "                    \"refName\": \"checkInboxFunction\"\n"
                    + "                }\n"
                    + "            }\n"
                    + "        ],\n"
                    + "        \"transition\": {\n"
                    + "            \"nextState\": \"SendTextForHighPrioriry\"\n"
                    + "        }\n"
                    + "    },\n"
                    + "    {\n"
                    + "        \"name\": \"SendTextForHighPrioriry\",\n"
                    + "        \"type\": \"foreach\",\n"
                    + "        \"inputCollection\": \"${ .message }\",\n"
                    + "        \"iterationParam\": \"${ .singlemessage }\",\n"
                    + "        \"end\": {\n"
                    + "            \"kind\": \"default\"\n"
                    + "        }\n"
                    + "    }\n"
                    + "]\n"
                    + "}")
            .validate();

    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());

    Assertions.assertTrue(
        validationErrors.stream()
            .anyMatch(
                v ->
                    v.getMessage()
                        .equals(
                            "Operation State action functionRef does not reference an existing workflow function definition")));
  }

  @Test
  public void testValidateWorkflowForOptionalStartStateAndWorkflowName() {
    Workflow workflow =
        new Workflow()
            .withId("test-workflow")
            .withVersion("1.0")
            .withStates(
                Arrays.asList(
                    new SleepState()
                        .withName("sleepState")
                        .withType(SLEEP)
                        .withEnd(new End())
                        .withDuration("PT1M")));

    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator.setWorkflow(workflow).validate();
    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(0, validationErrors.size());
  }

  @Test
  public void testValidateWorkflowForOptionalIterationParam() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "\"id\": \"checkInbox\",\n"
                    + "  \"name\": \"Check Inbox Workflow\",\n"
                    + "\"description\": \"Periodically Check Inbox\",\n"
                    + "\"version\": \"1.0\",\n"
                    + "\"start\": \"CheckInbox\",\n"
                    + "\"functions\": [\n"
                    + "\n"
                    + "],\n"
                    + "\"states\": [\n"
                    + "    {\n"
                    + "        \"name\": \"CheckInbox\",\n"
                    + "        \"type\": \"operation\",\n"
                    + "        \"actionMode\": \"sequential\",\n"
                    + "        \"actions\": [\n"
                    + "            {\n"
                    + "                \"functionRef\": {\n"
                    + "                    \"refName\": \"checkInboxFunction\"\n"
                    + "                }\n"
                    + "            }\n"
                    + "        ],\n"
                    + "        \"transition\": {\n"
                    + "            \"nextState\": \"SendTextForHighPrioriry\"\n"
                    + "        }\n"
                    + "    },\n"
                    + "    {\n"
                    + "        \"name\": \"SendTextForHighPrioriry\",\n"
                    + "        \"type\": \"foreach\",\n"
                    + "        \"inputCollection\": \"${ .message }\",\n"
                    + "        \"end\": {\n"
                    + "            \"kind\": \"default\"\n"
                    + "        }\n"
                    + "    }\n"
                    + "]\n"
                    + "}")
            .validate();

    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(1, validationErrors.size());
  }

  @Test
  public void testMissingFunctionRefForCallbackState() {
    WorkflowValidator workflowValidator = new WorkflowValidatorImpl();
    Collection<ValidationError> validationErrors =
        workflowValidator
            .setSource(
                "{\n"
                    + "  \"id\": \"callbackstatemissingfuncref\",\n"
                    + "  \"version\": \"1.0\",\n"
                    + "  \"specVersion\": \"0.8\",\n"
                    + "  \"name\": \"Callback State Test\",\n"
                    + "  \"start\": \"CheckCredit\",\n"
                    + "  \"states\": [\n"
                    + "    {\n"
                    + "      \"name\": \"CheckCredit\",\n"
                    + "      \"type\": \"callback\",\n"
                    + "      \"action\": {\n"
                    + "        \"functionRef\": {\n"
                    + "          \"refName\": \"callCreditCheckMicroservice\",\n"
                    + "          \"arguments\": {\n"
                    + "            \"customer\": \"${ .customer }\"\n"
                    + "          }\n"
                    + "        }\n"
                    + "      },\n"
                    + "      \"eventRef\": \"CreditCheckCompletedEvent\",\n"
                    + "      \"timeouts\": {\n"
                    + "        \"stateExecTimeout\": \"PT15M\"\n"
                    + "      },\n"
                    + "      \"end\": true\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}")
            .validate();

    Assertions.assertNotNull(validationErrors);
    Assertions.assertEquals(2, validationErrors.size());
    Assertions.assertEquals(
        validationErrors.stream()
            .filter(
                v ->
                    v.getMessage()
                            .equals(
                                "CallbackState event ref does not reference a defined workflow event definition")
                        || v.getMessage()
                            .equals(
                                "CallbackState action function ref does not reference a defined workflow function definition"))
            .count(),
        2);
  }
}
