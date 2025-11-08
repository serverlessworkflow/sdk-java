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
package io.serverlessworkflow.impl.executors.script.js;

import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.executors.ProcessResult;
import io.serverlessworkflow.impl.executors.RunScriptExecutor;
import io.serverlessworkflow.impl.executors.ScriptTaskRunner;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.function.BiFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class JavaScriptScriptTaskRunner implements ScriptTaskRunner {

  @Override
  public RunScriptExecutor.ScriptLanguage forLanguage() {
    return RunScriptExecutor.ScriptLanguage.JS;
  }

  @Override
  public BiFunction<RunScriptExecutor.RunScriptContext, WorkflowModel, WorkflowModel> buildRun(
      TaskContext taskContext) {
    return (script, input) -> {
      String js = forLanguage().getLang();
      WorkflowApplication application = script.getApplication();
      ByteArrayOutputStream stderr = new ByteArrayOutputStream();
      ByteArrayOutputStream stdout = new ByteArrayOutputStream();

      WorkflowModelFactory modelFactory = application.modelFactory();
      try (Context ctx =
          Context.newBuilder()
              .err(stderr)
              .out(stdout)
              .useSystemExit(true)
              .allowCreateProcess(false)
              .option("engine.WarnInterpreterOnly", "false")
              .build()) {

        script
            .getArgs()
            .forEach(
                (key, val) -> {
                  ctx.getBindings(js).putMember(key, val);
                });

        configureProcessEnv(ctx, script.getEnvs());

        if (!script.isAwait()) {
          script
              .getApplication()
              .executorService()
              .submit(
                  () -> {
                    ctx.eval(js, script.getCode());
                  });
          return application.modelFactory().fromAny(input);
        }

        ctx.eval(js, script.getCode());

        return switch (script.getReturnType()) {
          case ALL ->
              modelFactory.fromAny(new ProcessResult(0, stdout.toString(), stderr.toString()));
          case NONE -> modelFactory.fromNull();
          case CODE -> modelFactory.from(0);
          case STDOUT -> modelFactory.from(stdout.toString().trim());
          case STDERR -> modelFactory.from(stderr.toString().trim());
        };
      } catch (PolyglotException e) {
        if (e.getExitStatus() != 0 || e.isSyntaxError()) {
          throw new WorkflowException(WorkflowError.runtime(taskContext, e).build());
        } else {
          return switch (script.getReturnType()) {
            case ALL ->
                modelFactory.fromAny(
                    new ProcessResult(
                        e.getExitStatus(), stdout.toString().trim(), buildStderr(e, stderr)));
            case NONE -> modelFactory.fromNull();
            case CODE -> modelFactory.from(e.getExitStatus());
            case STDOUT -> modelFactory.from(stdout.toString().trim());
            case STDERR -> modelFactory.from(buildStderr(e, stderr));
          };
        }
      }
    };
  }

  /**
   * Gets the stderr message from the PolyglotException or the stderr stream.
   *
   * @param e the {@link PolyglotException} thrown during script execution
   * @param stderr the stderr stream
   * @return the stderr message
   */
  private String buildStderr(PolyglotException e, ByteArrayOutputStream stderr) {
    String err = stderr.toString();
    return err.isBlank() ? e.getMessage() : err.trim();
  }

  /**
   * Configures the process.env object in the JavaScript context with the provided environment
   * variables.
   *
   * @param context the GraalVM context
   * @param envs the environment variables to set
   */
  private void configureProcessEnv(Context context, Map<String, String> envs) {
    String js = RunScriptExecutor.ScriptLanguage.JS.getLang();
    Value bindings = context.getBindings(js);
    Value process = context.eval(js, "({ env: {} })");

    for (var entry : envs.entrySet()) {
      process.getMember("env").putMember(entry.getKey(), entry.getValue());
    }
    bindings.putMember("process", process);
  }
}
