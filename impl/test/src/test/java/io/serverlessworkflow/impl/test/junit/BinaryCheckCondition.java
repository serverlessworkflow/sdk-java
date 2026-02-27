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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class BinaryCheckCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return AnnotationUtils.findAnnotation(context.getElement(), DisabledIfBinaryUnavailable.class)
        .map(
            annotation -> {
              String[] binary = annotation.value();
              if (binary == null || binary.length == 0) {
                return ConditionEvaluationResult.enabled(
                    "No command found in the annotation @DisabledIfBinaryUnavailable");
              }
              if (isBinaryAvailable(binary)) {
                return ConditionEvaluationResult.enabled(binary[0] + " is available.");
              } else {
                return ConditionEvaluationResult.disabled(
                    "Test disabled: " + binary[0] + " not found.");
              }
            })
        .orElse(ConditionEvaluationResult.enabled("No @DisabledIfBinaryUnavailable found."));
  }

  public boolean isBinaryAvailable(String... command) {
    try {
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .start();
      boolean finished = process.waitFor(2, TimeUnit.SECONDS);
      if (finished) {
        return process.exitValue() == 0;
      }
      process.destroyForcibly();
      return false;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return false;
    }
  }
}
