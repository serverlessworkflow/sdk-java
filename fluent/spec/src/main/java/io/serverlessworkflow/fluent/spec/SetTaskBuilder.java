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
package io.serverlessworkflow.fluent.spec;

import io.serverlessworkflow.api.types.Set;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SetTaskConfiguration;

public class SetTaskBuilder extends TaskBaseBuilder<SetTaskBuilder> {

  private final SetTask setTask;
  private final SetTaskConfiguration setTaskConfiguration;

  public SetTaskBuilder() {
    this.setTask = new SetTask();
    this.setTaskConfiguration = new SetTaskConfiguration();
    this.setTask(setTask);
  }

  @Override
  protected SetTaskBuilder self() {
    return this;
  }

  public SetTaskBuilder expr(String expression) {
    this.setTask.setSet(new Set().withString(expression));
    return this;
  }

  public SetTaskBuilder put(String key, String value) {
    setTaskConfiguration.withAdditionalProperty(key, value);
    return this;
  }

  public SetTask build() {
    if (this.setTask.getSet() == null) {
      this.setTask.setSet(new Set().withSetTaskConfiguration(setTaskConfiguration));
    }

    return setTask;
  }
}
