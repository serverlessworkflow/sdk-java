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

import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.ContextFunction;
import io.serverlessworkflow.api.types.func.FilterFunction;
import io.serverlessworkflow.api.types.func.LoopFunction;
import io.serverlessworkflow.api.types.func.LoopFunctionIndex;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.executors.CallFunctionExecutorBuilder;
import io.serverlessworkflow.impl.executors.CallableTaskFactory;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class JavaCallFunctionBuilder extends CallFunctionExecutorBuilder {

  @Override
  public int priority() {
    return DEFAULT_PRIORITY - 10;
  }

  @Override
  public CallableTaskFactory init(
      CallFunction task, WorkflowDefinition definition, WorkflowMutablePosition position) {
    if (task.getCall().equals(CallJava.JAVA_CALL_KEY)) {
      Map<String, Object> props = task.getWith().getAdditionalProperties();
      Object obj = props.get(CallJava.FUNCTION_NAME_KEY);
      Optional<Class<?>> input =
          (Optional<Class<?>>) props.getOrDefault(CallJava.INPUT_CLASS_KEY, Optional.empty());
      Optional<Class<?>> output =
          (Optional<Class<?>>) props.getOrDefault(CallJava.OUTPUT_CLASS_KEY, Optional.empty());
      if (obj instanceof ContextFunction fn) {
        return () -> new JavaContextFunctionCallExecutor(input, output, fn);
      } else if (obj instanceof FilterFunction fn) {
        return () -> new JavaFilterFunctionCallExecutor(input, output, fn);
      } else if (obj instanceof LoopFunction loop) {
        return () ->
            new JavaLoopFunctionCallExecutor(
                loop, (String) props.get(CallJava.VAR_NAME_KEY), input, output);
      } else if (obj instanceof LoopFunctionIndex loop) {
        return () ->
            new JavaLoopFunctionIndexCallExecutor(
                loop,
                (String) props.get(CallJava.VAR_NAME_KEY),
                (String) props.get(CallJava.INDEX_NAME_KEY),
                input,
                output);

      } else if (obj instanceof Function fn) {
        return () -> new JavaFunctionCallExecutor(input, output, fn);
      } else if (obj instanceof Consumer consumer) {
        return () -> new JavaConsumerCallExecutor(input, consumer);
      } else {
        throw new UnsupportedOperationException("Unrecognized function " + obj);
      }
    } else {
      return super.init(task, definition, position);
    }
  }
}
