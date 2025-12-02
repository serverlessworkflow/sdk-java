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
package io.serverlessworkflow.impl.executors.script.python;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.config.ConfigManager;
import io.serverlessworkflow.impl.scripts.AbstractScriptRunner;
import io.serverlessworkflow.impl.scripts.ScriptContext;
import io.serverlessworkflow.impl.scripts.ScriptLanguageId;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import jep.Interpreter;
import jep.SharedInterpreter;

public class PythonScriptTaskRunner extends AbstractScriptRunner {

  @Override
  public ScriptLanguageId identifier() {
    return ScriptLanguageId.PYTHON;
  }

  private static final String PYTHON_SYS_PATH = "sys.path.append('%s')\n";
  private static final String SEARCH_PATH_PROPERTY = "io.serverlessworkflow.impl.";

  @Override
  protected void runScript(
      ScriptContext scriptContext,
      ByteArrayOutputStream stdout,
      ByteArrayOutputStream stderr,
      WorkflowContext workflowContext,
      TaskContext taskContext) {
    Interpreter py =
        workflowContext
            .instance()
            .additionalObject(
                "pyInterpreter",
                () -> interpreter(workflowContext.definition().application().configManager()));
    scriptContext.args().forEach(py::set);
    py.exec(scriptContext.code());
  }

  protected Interpreter interpreter(ConfigManager configManager) {
    Interpreter py = new SharedInterpreter();
    Collection<String> searchPaths = configManager.multiConfig(SEARCH_PATH_PROPERTY, String.class);
    if (!searchPaths.isEmpty()) {
      StringBuilder sb = new StringBuilder("import sys\n");
      searchPaths.forEach(path -> sb.append(String.format(PYTHON_SYS_PATH, path)));
      py.exec(sb.toString());
    }
    return py;
  }
}
