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
import io.serverlessworkflow.api.validation.ValidationError;
import java.util.Collection;

abstract class CommonStateValidator<T extends State> implements StateValidator {

  static final String EMPTY_STATE_NAME_MSG = "State name should not be empty";

  private final Collection<ValidationError> validationErrors;

  protected CommonStateValidator(Collection<ValidationError> validationErrors) {
    this.validationErrors = validationErrors;
  }

  @Override
  public final boolean isValidState(Workflow workflow, State state) {
    @SuppressWarnings("unchecked")
    T typedState = (T) state;

    return isValidSpecificState(workflow, typedState) && isStateNameValid(state);
  }

  private boolean isStateNameValid(State state) {
    if (state.getName() != null && !state.getName().trim().isEmpty()) {
      return true;
    } else {
      addError(EMPTY_STATE_NAME_MSG);
      return false;
    }
  }

  protected final void addError(String message) {
    ValidationError error = new ValidationError();
    error.setType(ValidationError.WORKFLOW_VALIDATION);
    error.setMessage(message);
    validationErrors.add(error);
  }

  protected abstract boolean isValidSpecificState(Workflow workflow, T state);
}
