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

import static io.serverlessworkflow.impl.executors.func.JavaCallExecutor.safeObject;

import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.api.types.func.ForTaskFunction;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowFilter;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.WorkflowUtils;
import io.serverlessworkflow.impl.executors.ForExecutor.ForExecutorBuilder;
import io.serverlessworkflow.impl.expressions.LoopPredicateIndex;
import io.serverlessworkflow.impl.resources.ResourceLoader;
import java.util.Optional;

public class JavaForExecutorBuilder extends ForExecutorBuilder {

  protected JavaForExecutorBuilder(
      WorkflowPosition position,
      ForTask task,
      Workflow workflow,
      WorkflowApplication application,
      ResourceLoader resourceLoader) {
    super(position, task, workflow, application, resourceLoader);
    if (task instanceof ForTaskFunction taskFunctions) {}
  }

  protected Optional<WorkflowFilter> buildWhileFilter() {
    if (task instanceof ForTaskFunction taskFunctions) {
      LoopPredicateIndex whilePred = taskFunctions.getWhilePredicate();
      String varName = task.getFor().getEach();
      String indexName = task.getFor().getAt();
      if (whilePred != null) {
        return Optional.of(
            (w, t, n) -> {
              Object item = safeObject(t.variables().get(varName));
              return application
                  .modelFactory()
                  .from(
                      whilePred.test(
                          n.asJavaObject(),
                          item,
                          (Integer) safeObject(t.variables().get(indexName))));
            });
      }
    }
    return super.buildWhileFilter();
  }

  protected WorkflowFilter buildCollectionFilter() {
    return task instanceof ForTaskFunction taskFunctions
        ? WorkflowUtils.buildWorkflowFilter(application, null, taskFunctions.getCollection())
        : super.buildCollectionFilter();
  }
}
