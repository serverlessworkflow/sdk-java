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

import io.serverlessworkflow.api.types.SwitchCase;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.SwitchCaseFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.executors.SwitchExecutor.SwitchExecutorBuilder;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Optional;

public class JavaSwitchExecutorBuilder extends SwitchExecutorBuilder {

  protected JavaSwitchExecutorBuilder(
      WorkflowPosition position,
      SwitchTask task,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    super(position, task, workflow, application, resourceLoader);
  }

  @Override
  protected Optional<WorkflowFilter> buildFilter(SwitchCase switchCase) {
    return switchCase instanceof SwitchCaseFunction function
        ? Optional.of(WorkflowUtils.buildWorkflowFilter(application, null, function.predicate()))
        : super.buildFilter(switchCase);
  }
}
