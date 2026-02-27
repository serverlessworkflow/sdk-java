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

import static io.serverlessworkflow.impl.test.junit.BinaryCheckUtil.isBinaryAvailable;

import com.github.os72.protocjar.Protoc;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class ProtocCheckCondition implements ExecutionCondition {

  private static final AtomicReference<Boolean> protocAvailable = new AtomicReference<>(null);

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return AnnotationUtils.findAnnotation(context.getElement(), DisabledIfProtocUnavailable.class)
        .map(
            annotation -> {
              if (isProtocAvailable()) {
                return ConditionEvaluationResult.enabled(
                    "Protoc is available for the current platform.");
              } else {
                return ConditionEvaluationResult.disabled(
                    "Test disabled: Protoc not currently available not found.");
              }
            })
        .orElse(ConditionEvaluationResult.enabled("No @DisabledIfProtocUnavailable found."));
  }

  private boolean isProtocAvailable() {
    Boolean cached = protocAvailable.get();
    if (cached != null) {
      return cached;
    }

    boolean found = false;
    try {
      if (Protoc.runProtoc(new String[] {"--version"}) == 0) found = true;
    } catch (Exception e) {
      // ignore
    }

    if (!found) found = isBinaryAvailable("protoc", "--version");

    protocAvailable.compareAndSet(null, found);

    return protocAvailable.get();
  }
}
