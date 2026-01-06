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
package io.serverlessworkflow.fluent.processes;

import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Objects;
import java.util.function.Predicate;

public class FlexibleProcess extends TaskBase {

  private Predicate<WorkflowModel> exitCondition;
  private Activity[] activities;

  private int maxAttempts;

  private FlexibleProcess(Predicate<WorkflowModel> exitCondition, Activity[] activities) {
    this.exitCondition = exitCondition;
    this.activities = activities;
  }

  public Predicate<WorkflowModel> getExitCondition() {
    return exitCondition;
  }

  public Activity[] getActivities() {
    Activity[] result = new Activity[activities.length];
    for (int i = 0; i < activities.length; i++) {
      result[i] = activities[i].newInstance();
    }
    return result;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public Task asTask() {
    Task task = new Task();
    task.setCallTask(new FlexibleProcessCallTask(this));
    return task;
  }

  public static FlexibleProcessBuilder builder() {
    return new FlexibleProcessBuilder();
  }

  public static class FlexibleProcessBuilder {
    private Predicate<WorkflowModel> exitCondition;
    private Activity[] activities;
    private int maxAttempts = 1024;

    public FlexibleProcessBuilder exitCondition(Predicate<WorkflowModel> exitCondition) {
      this.exitCondition = exitCondition;
      return this;
    }

    public FlexibleProcessBuilder activities(Activity... activities) {
      this.activities = activities;
      return this;
    }

    public FlexibleProcessBuilder maxAttempts(int maxAttempts) {
      if (maxAttempts < 1) {
        throw new IllegalArgumentException("maxAttempts must be greater than 0");
      }
      this.maxAttempts = maxAttempts;
      return this;
    }

    public FlexibleProcess build() {
      FlexibleProcess flexibleProcess =
          new FlexibleProcess(
              Objects.requireNonNull(exitCondition, "Exit condition must be provided"),
              Objects.requireNonNull(activities, "Activities must be provided"));
      flexibleProcess.maxAttempts = maxAttempts;
      return flexibleProcess;
    }
  }
}
