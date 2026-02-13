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
package io.serverlessworkflow.impl.scheduler;

import io.serverlessworkflow.impl.WorkflowDefinition;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.function.Consumer;

public class ScheduledInstanceRunnable implements Runnable, Consumer<WorkflowModel> {

  protected final WorkflowDefinition definition;

  public ScheduledInstanceRunnable(WorkflowDefinition definition) {
    this.definition = definition;
  }

  @Override
  public void run() {
    accept(definition.application().modelFactory().fromNull());
  }

  @Override
  public void accept(WorkflowModel model) {
    runScheduledInstance(definition, model);
  }

  public static void runScheduledInstance(WorkflowDefinition definition, WorkflowModel model) {
    WorkflowInstance instance = definition.instance(model);
    definition.addScheduledInstance(instance);
    definition.application().executorService().execute(() -> instance.start());
  }
}
