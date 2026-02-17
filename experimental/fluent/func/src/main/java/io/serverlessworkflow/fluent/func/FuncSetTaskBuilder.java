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
package io.serverlessworkflow.fluent.func;

import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.func.MapSetTaskConfiguration;
import io.serverlessworkflow.fluent.func.spi.ConditionalTaskBuilder;
import io.serverlessworkflow.fluent.spec.SetTaskBuilder;
import java.util.Map;

public class FuncSetTaskBuilder extends SetTaskBuilder
    implements ConditionalTaskBuilder<FuncSetTaskBuilder> {

  private final SetTask task;

  FuncSetTaskBuilder() {
    this.task = new SetTask();
    this.setTask(task);
  }

  public FuncSetTaskBuilder expr(Map<String, Object> map) {
    if (this.task.getSet() == null) {
      this.task.setSet(new Set());
    }
    this.task.getSet().withSetTaskConfiguration(new MapSetTaskConfiguration(map));
    return this;
  }

  @Override
  public SetTask build() {
    return this.task;
  }
}
