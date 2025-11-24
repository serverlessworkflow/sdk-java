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
package io.serverlessworkflow.impl.executors;

import io.serverlessworkflow.api.types.CallFunction;
import io.serverlessworkflow.api.types.FunctionArguments;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowMutablePosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.WorkflowValueResolver;
import java.util.Map;
import java.util.Optional;

public class CallFunctionExecutor implements CallableTaskBuilder<CallFunction> {

  private TaskExecutorBuilder<? extends TaskBase> executorBuilder;
  private WorkflowValueResolver<Map<String, Object>> args;

  @Override
  public void init(
      CallFunction task, WorkflowDefinition definition, WorkflowMutablePosition position) {
    String functionName = task.getCall();
    FunctionArguments functionArgs = task.getWith();
    args =
        functionArgs != null
            ? WorkflowUtils.buildMapResolver(
                definition.application(), functionArgs.getAdditionalProperties())
            : (w, t, m) -> Map.of();
    Task function = null;
    if (definition.workflow().getUse() != null
        && definition.workflow().getUse().getFunctions() != null
        && definition.workflow().getUse().getFunctions().getAdditionalProperties() != null) {
      function =
          definition.workflow().getUse().getFunctions().getAdditionalProperties().get(functionName);
    }
    if (function == null) {
      // TODO search in catalog
      throw new UnsupportedOperationException("Function Catalog not supported yet");
    }
    executorBuilder =
        definition.application().taskFactory().getTaskExecutor(position, function, definition);
  }

  @Override
  public boolean accept(Class<? extends TaskBase> clazz) {
    return clazz.equals(CallFunction.class);
  }

  @Override
  public CallableTask build() {
    TaskExecutor<? extends TaskBase> executor = executorBuilder.build();
    return (w, t, m) ->
        executor
            .apply(
                w,
                Optional.of(t),
                w.definition().application().modelFactory().fromAny(args.apply(w, t, m)))
            .thenApply(o -> o.output());
  }
}
