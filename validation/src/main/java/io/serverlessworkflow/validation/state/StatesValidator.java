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
import io.serverlessworkflow.api.interfaces.State;
import io.serverlessworkflow.api.states.DefaultState.Type;
import io.serverlessworkflow.api.validation.ValidationError;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StatesValidator {
  private final Map<Type, StateValidator> stateValidators = new EnumMap<>(Type.class);

  private final Collection<ValidationError> validationErrors;

  public StatesValidator(Collection<ValidationError> validationErrors) {
    this.validationErrors = validationErrors;

    stateValidators.put(Type.EVENT, new EventStateValidator(validationErrors));
    stateValidators.put(Type.OPERATION, new OperationStateValidator(validationErrors));
    stateValidators.put(Type.SWITCH, new SwitchStateValidator(validationErrors));
    stateValidators.put(Type.SLEEP, new SleepStateValidator(validationErrors));
    stateValidators.put(Type.PARALLEL, new ParallelStateValidator(validationErrors));
    stateValidators.put(Type.INJECT, new InjectStateValidator(validationErrors));
    stateValidators.put(Type.FOREACH, new ForEachStateValidator(validationErrors));
    stateValidators.put(Type.CALLBACK, new CallbackStateValidator(validationErrors));
  }

  public boolean hasValidStates(Workflow workflow) {
    boolean finalValidationResult = true;

    List<State> states = workflow.getStates();
    if (states != null && !states.isEmpty()) {
      for (State state : states) {
        StateValidator stateValidator = stateValidators.get(state.getType());

        if (stateValidator != null) {
          finalValidationResult =
              finalValidationResult && stateValidator.isValidState(workflow, state);
        } else {
          throw new IllegalStateException("No validator found for state type: " + state.getType());
        }
      }
    }

    return finalValidationResult;
  }
}
