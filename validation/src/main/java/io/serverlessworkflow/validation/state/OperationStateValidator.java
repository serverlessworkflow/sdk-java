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
import io.serverlessworkflow.api.actions.Action;
import io.serverlessworkflow.api.events.EventDefinition;
import io.serverlessworkflow.api.functions.FunctionDefinition;
import io.serverlessworkflow.api.states.OperationState;
import io.serverlessworkflow.api.validation.ValidationError;
import java.util.Collection;
import java.util.List;

final class OperationStateValidator extends CommonStateValidator<OperationState> {

  OperationStateValidator(Collection<ValidationError> validationErrors) {
    super(validationErrors);
  }

  @Override
  protected boolean isValidSpecificState(Workflow workflow, OperationState operationState) {
    boolean isValidSpecificState = true;

    List<FunctionDefinition> functions =
        workflow.getFunctions() != null ? workflow.getFunctions().getFunctionDefs() : null;

    List<EventDefinition> events =
        workflow.getEvents() != null ? workflow.getEvents().getEventDefs() : null;

    List<Action> actions = operationState.getActions();
    for (Action action : actions) {
      if (action.getFunctionRef() != null) {
        if (action.getFunctionRef().getRefName().isEmpty()) {
          addError("Operation State action functionRef should not be null or empty");
          isValidSpecificState = false;
        }

        if (!hasFunctionDefinition(action.getFunctionRef().getRefName(), functions)) {
          addError(
              "Operation State action functionRef does not reference an existing workflow function definition");
          isValidSpecificState = false;
        }
      }

      if (action.getEventRef() != null) {
        if (action.getEventRef().getTriggerEventRef().isEmpty()) {
          addError(
              "Operation State action trigger eventRef does not reference an existing workflow event definition");
          isValidSpecificState = false;
        }

        if (action.getEventRef().getResultEventRef().isEmpty()) {
          addError(
              "Operation State action results eventRef does not reference an existing workflow event definition");
          isValidSpecificState = false;
        }

        if (doesntHaveEventsDefinition(action.getEventRef().getTriggerEventRef(), events)) {
          addError(
              "Operation State action trigger event def does not reference an existing workflow event definition");
          isValidSpecificState = false;
        }

        if (doesntHaveEventsDefinition(action.getEventRef().getResultEventRef(), events)) {
          addError(
              "Operation State action results event def does not reference an existing workflow event definition");
          isValidSpecificState = false;
        }
      }
    }
    return isValidSpecificState;
  }

  private boolean doesntHaveEventsDefinition(String eventName, List<EventDefinition> events) {
    if (events != null) {
      EventDefinition eve =
          events.stream().filter(e -> e.getName().equals(eventName)).findFirst().orElse(null);

      return eve == null;
    } else {
      return true;
    }
  }

  private boolean hasFunctionDefinition(String functionName, List<FunctionDefinition> functions) {
    if (functions != null) {
      FunctionDefinition fun =
          functions.stream().filter(f -> f.getName().equals(functionName)).findFirst().orElse(null);

      return fun != null;
    } else {
      return false;
    }
  }
}
