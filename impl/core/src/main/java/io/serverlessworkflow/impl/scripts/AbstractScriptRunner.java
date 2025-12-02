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
package io.serverlessworkflow.impl.scripts;

import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.executors.ProcessResult;
import java.io.ByteArrayOutputStream;

public abstract class AbstractScriptRunner implements ScriptRunner {

  @Override
  public WorkflowModel runScript(
      ScriptContext scriptContext,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    try {
      runScript(scriptContext, stdout, stderr, workflowContext, taskContext);
      return scriptContext
          .returnType()
          .map(
              type ->
                  modelFromOutput(
                      type,
                      workflowContext.definition().application().modelFactory(),
                      stdout,
                      stderr))
          .orElse(input);
    } catch (Exception ex) {
      throw new WorkflowException(WorkflowError.runtime(taskContext, ex).build());
    }
  }

  protected abstract void runScript(
      ScriptContext scriptContext,
      ByteArrayOutputStream stdout,
      ByteArrayOutputStream stderr,
      WorkflowContext workflowContext,
      TaskContext taskContext);

  protected WorkflowModel modelFromOutput(
      RunTaskConfiguration.ProcessReturnType returnType,
      WorkflowModelFactory modelFactory,
      ByteArrayOutputStream stdout,
      ByteArrayOutputStream stderr) {
    return switch (returnType) {
      case ALL -> modelFactory.fromAny(new ProcessResult(0, toString(stdout), toString(stderr)));
      case NONE -> modelFactory.fromNull();
      case CODE -> modelFactory.from(0);
      case STDOUT -> modelFactory.from(toString(stdout));
      case STDERR -> modelFactory.from(toString(stderr));
    };
  }

  private String toString(ByteArrayOutputStream stream) {
    return stream.toString().trim();
  }
}
