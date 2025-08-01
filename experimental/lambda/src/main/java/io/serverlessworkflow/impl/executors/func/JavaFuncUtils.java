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
package io.serverlessworkflow.impl.executors.func;

import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Optional;

public class JavaFuncUtils {

  static Object safeObject(Object obj) {
    return obj instanceof WorkflowModel model ? model.asJavaObject() : obj;
  }

  static <T> T convertT(WorkflowModel model, Optional<Class<T>> inputClass) {
    return inputClass
        .map(
            c ->
                model
                    .as(c)
                    .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                "Model " + model + " cannot be converted to type " + c)))
        .orElseGet(() -> (T) model.asJavaObject());
  }

  static Object convert(WorkflowModel model, Optional<Class<?>> inputClass) {
    return inputClass.isPresent()
        ? model
            .as(inputClass.orElseThrow())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Model " + model + " cannot be converted to type " + inputClass))
        : model.asJavaObject();
  }

  private JavaFuncUtils() {}
}
