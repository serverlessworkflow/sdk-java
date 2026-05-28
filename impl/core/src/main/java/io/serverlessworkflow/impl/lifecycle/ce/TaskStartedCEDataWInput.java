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
package io.serverlessworkflow.impl.lifecycle.ce;

import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;

public class TaskStartedCEDataWInput extends TaskStartedCEData {
  private Object input;

  public TaskStartedCEDataWInput(TaskStartedEvent ev) {
    super(ev);
    this.input = ev.taskContext().input();
  }

  public TaskStartedCEDataWInput() {}

  public Object input() {
    return input;
  }

  public Object getInput() {
    return input;
  }
}
