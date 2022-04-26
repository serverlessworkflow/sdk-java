/*
 * Copyright 2022-Present The Serverless Workflow Specification Authors
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
package io.serverlessworkflow.validation.state;

import static io.serverlessworkflow.validation.state.SwitchStateValidator.DATA_CONDITIONS_AND_EVENT_CONDITIONS_FOUND_ERROR;
import static io.serverlessworkflow.validation.state.SwitchStateValidator.EVENT_TIMEOUT_REQUIRED_ERROR;
import static io.serverlessworkflow.validation.state.SwitchStateValidator.INVALID_EVENT_TIMEOUT_ERROR;
import static io.serverlessworkflow.validation.state.SwitchStateValidator.NEXT_STATE_NOT_FOUND_FOR_DEFAULT_CONDITION_ERROR;
import static io.serverlessworkflow.validation.state.SwitchStateValidator.NEXT_STATE_REQUIRED_FOR_DEFAULT_CONDITION_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.validation.ValidationError;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SwitchStateValidatorTest extends StateValidatorTestUtil {

  public static Stream<Arguments> testInvalidWorkflowSource() {
    return Stream.of(
        Arguments.of(
            "/states/switchstate/invalid/data_condition_and_event_condition_present.json",
            String.format(DATA_CONDITIONS_AND_EVENT_CONDITIONS_FOUND_ERROR, "CheckVisaStatus")),
        Arguments.of(
            "/states/switchstate/defaultcondition/invalid/next_state_empty.json",
            String.format(
                NEXT_STATE_REQUIRED_FOR_DEFAULT_CONDITION_ERROR,
                "CheckVisaStatus",
                "Event Based Switch Transitions")),
        Arguments.of(
            "/states/switchstate/defaultcondition/invalid/next_state_null.json",
            String.format(
                NEXT_STATE_REQUIRED_FOR_DEFAULT_CONDITION_ERROR,
                "CheckVisaStatus",
                "Event Based Switch Transitions")),
        Arguments.of(
            "/states/switchstate/defaultcondition/invalid/next_state_present_but_non_existing.json",
            String.format(
                NEXT_STATE_NOT_FOUND_FOR_DEFAULT_CONDITION_ERROR,
                "NonExistingNextState",
                "CheckVisaStatus",
                "Event Based Switch Transitions")),
        Arguments.of(
            "/states/switchstate/eventcondition/invalid/missing_event_timeout.json",
            String.format(
                EVENT_TIMEOUT_REQUIRED_ERROR,
                "CheckContinueVitalChecks",
                "Check Car Vitals Workflow")),
        Arguments.of(
            "/states/switchstate/timeouts/eventtimeout/duration/invalid/state_timeout_duration.json",
            String.format(
                INVALID_EVENT_TIMEOUT_ERROR,
                "Serverless Workflow is Awesome",
                "Event Based Switch Transitions")),
        Arguments.of(
            "/states/switchstate/timeouts/eventtimeout/duration/invalid/workflow_timeout_duration.json",
            String.format(
                INVALID_EVENT_TIMEOUT_ERROR,
                "Serverless Workflow is Awesome",
                "Event Based Switch Transitions")));
  }

  public static Stream<Arguments> testValidWorkflowSource() {
    return Stream.of(
        Arguments.of("/states/switchstate/defaultcondition/valid/next_state_present.json"),
        Arguments.of("/states/switchstate/defaultcondition/valid/next_state_end.json"),
        Arguments.of("/states/switchstate/timeouts/eventtimeout/valid/event_timeout_on_state.json"),
        Arguments.of(
            "/states/switchstate/timeouts/eventtimeout/valid/event_timeout_on_workflow.json"),
        Arguments.of(
            "/states/switchstate/timeouts/eventtimeout/valid/missing_event_timeout_on_state_without_event_condition.json"),
        Arguments.of("/states/switchstate/timeouts/eventtimeout/duration/valid/state_timeout.json"),
        Arguments.of(
            "/states/switchstate/timeouts/eventtimeout/duration/valid/workflow_timeout.json"));
  }

  @ParameterizedTest
  @MethodSource("testInvalidWorkflowSource")
  void testInvalidWorkflow(String workflowFilePath, String expectedError) throws IOException {
    Collection<ValidationError> errors = validateFile(workflowFilePath);

    assertThat(errors).hasSize(1).allMatch(error -> expectedError.equals(error.getMessage()));
  }

  @ParameterizedTest
  @MethodSource("testValidWorkflowSource")
  void testValidWorkflow(String workflowFilePath) throws IOException {
    Collection<ValidationError> errors = validateFile(workflowFilePath);
    assertThat(errors).isEmpty();
  }
}
