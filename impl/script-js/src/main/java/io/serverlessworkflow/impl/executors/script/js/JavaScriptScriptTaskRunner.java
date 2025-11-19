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

import io.serverlessworkflow.api.types.RunTaskConfiguration;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowError;
import io.serverlessworkflow.impl.WorkflowException;
import io.serverlessworkflow.impl.WorkflowModel;
import io.serverlessworkflow.impl.WorkflowModelFactory;
import io.serverlessworkflow.impl.executors.ProcessResult;
import io.serverlessworkflow.impl.scripts.ScriptContext;
import io.serverlessworkflow.impl.scripts.ScriptLanguageId;
import io.serverlessworkflow.impl.scripts.ScriptRunner;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.function.Supplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * JavaScript implementation of the {@link ScriptRunner} interface that executes JavaScript scripts
 * using GraalVM Polyglot API.
 */
public class JavaScriptScriptTaskRunner implements ScriptRunner {

  @Override
  public ScriptLanguageId identifier() {
    return ScriptLanguageId.JS;
  }

  @Override
  public WorkflowModel runScript(
      ScriptContext script,
      WorkflowContext workflowContext,
      TaskContext taskContext,
      WorkflowModel input) {
    WorkflowApplication application = workflowContext.definition().application();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    try (Context ctx =
        Context.newBuilder()
            .err(stderr)
            .out(stdout)
            .useSystemExit(true)
            .allowCreateProcess(false)
            .option("engine.WarnInterpreterOnly", "false")
            .build()) {

      script
          .args()
          .forEach(
              (key, val) -> {
                ctx.getBindings(identifier().getLang()).putMember(key, val);
              });
      configureProcessEnv(ctx, script.envs());
      ctx.eval(Source.create(identifier().getLang(), script.code()));
      return script
          .returnType()
          .map(
              type ->
                  modelFromOutput(
                      type, application.modelFactory(), stdout, () -> stderr.toString()))
          .orElse(input);
    } catch (PolyglotException e) {
      if (e.getExitStatus() != 0 || e.isSyntaxError()) {
        throw new WorkflowException(WorkflowError.runtime(taskContext, e).build());
      } else {
        return script
            .returnType()
            .map(
                type ->
                    modelFromOutput(
                        type, application.modelFactory(), stdout, () -> buildStderr(e, stderr)))
            .orElse(input);
      }
    }
  }

  private WorkflowModel modelFromOutput(
      RunTaskConfiguration.ProcessReturnType returnType,
      WorkflowModelFactory modelFactory,
      ByteArrayOutputStream stdout,
      Supplier<String> stderr) {
    return switch (returnType) {
      case ALL ->
          modelFactory.fromAny(new ProcessResult(0, stdout.toString().trim(), stderr.get().trim()));
      case NONE -> modelFactory.fromNull();
      case CODE -> modelFactory.from(0);
      case STDOUT -> modelFactory.from(stdout.toString().trim());
      case STDERR -> modelFactory.from(stderr.get().trim());
    };
  }

  /*
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

  /*
   * Configures the process.env object in the JavaScript context with the provided environment
   * variables.
   *
   * @param context the GraalVM context
   * @param envs the environment variables to set
   */
  private void configureProcessEnv(Context context, Map<String, Object> envs) {
    String js = ScriptLanguageId.JS.getLang();
    Value bindings = context.getBindings(js);
    Value process = context.eval(js, "({ env: {} })");

    for (var entry : envs.entrySet()) {
      process.getMember("env").putMember(entry.getKey(), entry.getValue());
    }
    bindings.putMember("process", process);
  }
}
