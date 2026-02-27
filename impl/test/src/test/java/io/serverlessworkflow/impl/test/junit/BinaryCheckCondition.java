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
package io.serverlessworkflow.impl.test.junit;

import static io.serverlessworkflow.impl.test.junit.BinaryAvailabilityUtil.isBinaryAvailable;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public class BinaryCheckCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    String[] command =
        context
            .getElement()
            .map(el -> el.getAnnotation(DisabledIfBinaryUnavailable.class).value())
            .orElse(null);
    if (command == null || command.length == 0) {
      throw new IllegalArgumentException("No @DisabledIfBinaryUnavailable annotation is present");
    }

    if (isBinaryAvailable(command)) {
      return ConditionEvaluationResult.enabled(command[0] + " is available on this system.");
    } else {
      return ConditionEvaluationResult.disabled(
          "Test disabled: " + command[0] + " command not found.");
    }
  }
}
