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

import io.serverlessworkflow.api.Workflow;
import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.SwitchState;
import io.serverlessworkflow.api.timeouts.TimeoutsDefinition;
import io.serverlessworkflow.api.validation.ValidationError;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class SwitchStateValidator extends CommonStateValidator<SwitchState> {

  private static final String NON_NEGATIVE_DURATION_MUST_BE_PROVIDED =
      "When configured, it must be set with a greater than zero ISO 8601 time duration. For example PT30S.";

  static final String INVALID_EVENT_TIMEOUT_ERROR =
      "An invalid \"eventTimeout\": \"%s\" configuration was provided for the serverless workflow: \"%s\". "
          + NON_NEGATIVE_DURATION_MUST_BE_PROVIDED;

  static final String EVENT_TIMEOUT_REQUIRED_ERROR =
      "The \"eventTimeout\" configuration is required for the \"eventConditions\" based switch state \"%s\" that belongs to the serverless workflow: \"%s\".";

  static final String DATA_CONDITIONS_AND_EVENT_CONDITIONS_FOUND_ERROR =
      "DataConditions and eventConditions where found at the same time for the switch state \"%s\".";

  static final String NEXT_STATE_NOT_FOUND_FOR_DEFAULT_CONDITION_ERROR =
      "The \"nextState\" : \"%s\" configured for the \"defaultCondition\" transition in the switch state \"%s\", was not found in the serverless workflow: \"%s\".";

  static final String NEXT_STATE_REQUIRED_FOR_DEFAULT_CONDITION_ERROR =
      "The \"nextState\" is required for the \"defaultCondition\" transition in the switch state \"%s\" that belongs to the serverless workflow: \"%s\".";

  SwitchStateValidator(Collection<ValidationError> validationErrors) {
    super(validationErrors);
  }

  @Override
  protected boolean isValidSpecificState(Workflow workflow, SwitchState state) {
    return isValidDefaultCondition(workflow, state)
        && isValidEventCondition(workflow, state)
        && isOnlyDataConditionOrOnlyEventConditionPresent(state);
  }

  private boolean isOnlyDataConditionOrOnlyEventConditionPresent(SwitchState switchState) {
    if (!switchState.getDataConditions().isEmpty() && !switchState.getEventConditions().isEmpty()) {
      addError(
          String.format(DATA_CONDITIONS_AND_EVENT_CONDITIONS_FOUND_ERROR, switchState.getName()));
      return false;
    } else {
      return true;
    }
  }

  private boolean isDefaultConditionNextStateFound(
      Workflow workflow, String switchStateName, String nextState) {
    if (nextState != null && !stateExists(workflow, nextState) && !"end".equals(nextState)) {
      addError(
          String.format(
              NEXT_STATE_NOT_FOUND_FOR_DEFAULT_CONDITION_ERROR,
              nextState,
              switchStateName,
              workflow.getName()));

      return false;
    } else {
      return true;
    }
  }

  private static boolean stateExists(Workflow workflow, String state) {
    List<State> states = workflow.getStates();
    if (states != null) {
      return states.stream()
          .map(State::getName)
          .filter(Objects::nonNull)
          .anyMatch(name -> name.equals(state));
    } else {
      return false;
    }
  }

  private boolean isValidEventTimeoutDuration(Workflow workflow, String timeout) {
    if (isInvalidDuration(timeout)) {
      addError(String.format(INVALID_EVENT_TIMEOUT_ERROR, timeout, workflow.getName()));
      return false;
    } else {
      return true;
    }
  }

  private static boolean isInvalidDuration(String timeout) {
    try {
      Duration.parse(timeout);
      return false;
    } catch (DateTimeParseException e) {
      return true;
    }
  }

  private static boolean switchStateHasEventCondition(SwitchState switchState) {
    return switchState.getEventConditions() != null && !switchState.getEventConditions().isEmpty();
  }

  private static Optional<String> resolveEventTimeout(SwitchState switchState, Workflow workflow) {
    return resolveStateTimeout(switchState).or(() -> resolveWorkflowTimeout(workflow));
  }

  private static Optional<String> resolveStateTimeout(SwitchState switchState) {
    return Optional.ofNullable(switchState.getTimeouts()).map(TimeoutsDefinition::getEventTimeout);
  }

  private static Optional<String> resolveWorkflowTimeout(Workflow workflow) {
    return Optional.ofNullable(workflow.getTimeouts()).map(TimeoutsDefinition::getEventTimeout);
  }

  private boolean isValidNextState(
      Workflow workflow, String switchStateName, DefaultConditionDefinition defaultCondition) {
    String nextState =
        defaultCondition.getTransition() != null
            ? defaultCondition.getTransition().getNextState()
            : null;
    if (nextState != null && !nextState.isEmpty()) {
      return isDefaultConditionNextStateFound(workflow, switchStateName, nextState);
    } else {
      addError(
          String.format(
              NEXT_STATE_REQUIRED_FOR_DEFAULT_CONDITION_ERROR,
              switchStateName,
              workflow.getName()));

      return false;
    }
  }

  private boolean isValidDefaultCondition(Workflow workflow, SwitchState switchState) {
    DefaultConditionDefinition defaultCondition = switchState.getDefaultCondition();
    if (defaultCondition != null) {
      return isValidNextState(workflow, switchState.getName(), defaultCondition);
    } else {
      return true;
    }
  }

  private boolean isValidEventCondition(Workflow workflow, SwitchState switchState) {
    if (switchStateHasEventCondition(switchState)) {
      Optional<String> eventTimeout = resolveEventTimeout(switchState, workflow);
      if (eventTimeout.isPresent()) {
        return isValidEventTimeoutDuration(workflow, eventTimeout.orElseThrow());
      } else {
        addError(
            String.format(EVENT_TIMEOUT_REQUIRED_ERROR, switchState.getName(), workflow.getName()));
        return false;
      }
    } else {
      return true;
    }
  }
}
