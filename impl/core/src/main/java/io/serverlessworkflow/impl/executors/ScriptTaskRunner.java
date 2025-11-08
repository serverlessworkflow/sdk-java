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

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.function.BiFunction;

/** Represents a script task that executes a script in a specific scripting language. */
public interface ScriptTaskRunner {

  /**
   * The scripting language supported by this script task runner.
   *
   * @return the scripting language as {@link RunScriptExecutor.LanguageId} enum.
   */
  RunScriptExecutor.LanguageId identifier();

  /**
   * Returns a function that executes the script task.
   *
   * @param taskContext the task context for the script task.
   * @return a @{@link BiFunction}} that takes a RunScriptContext and a WorkflowModel as input and
   *     returns a WorkflowModel as output.
   */
  BiFunction<RunScriptExecutor.RunScriptContext, WorkflowModel, WorkflowModel> buildRun(
      TaskContext taskContext);
}
