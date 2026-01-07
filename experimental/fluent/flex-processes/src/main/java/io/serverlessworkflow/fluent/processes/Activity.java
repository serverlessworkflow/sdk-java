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

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.func.CallJava;
import io.serverlessworkflow.api.types.func.CallTaskJava;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Activity {

  private final boolean isRepeatable;
  private final Task task;
  private final Predicate<WorkflowModel> entryCondition;
  private final String name;
  private Consumer<WorkflowModel> postAction;
  private boolean executed;

  Activity(
      Task task,
      String name,
      Predicate<WorkflowModel> entryCondition,
      Consumer<WorkflowModel> postAction,
      boolean isRepeatable) {
    this.task = task;
    this.name = name;
    this.entryCondition = entryCondition;
    this.postAction = postAction;
    this.isRepeatable = isRepeatable;
  }

  public Task getTask() {
    return task;
  }

  public Predicate<WorkflowModel> getEntryCondition() {
    return entryCondition;
  }

  public Consumer<WorkflowModel> getPostAction() {
    return postAction;
  }

  public String getName() {
    return name;
  }

  public boolean isRepeatable() {
    return isRepeatable;
  }

  public boolean isExecuted() {
    return executed;
  }

  public void setExecuted() {
    this.executed = true;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    Activity activity = (Activity) object;
    return Objects.equals(task, activity.task)
        && Objects.equals(entryCondition, activity.entryCondition)
        && Objects.equals(postAction, activity.postAction);
  }

  public Activity copy() {
    return new Activity(
        this.task, this.name, this.entryCondition, this.postAction, this.isRepeatable);
  }

  public static ActivityBuilder builder() {
    return new ActivityBuilder();
  }

  public static class ActivityBuilder {
    private Task task;
    private String name;
    private Predicate<WorkflowModel> entryCondition;
    private Consumer<WorkflowModel> postAction;
    private boolean isRepeatable;

    private ActivityBuilder() {}

    public ActivityBuilder callTask(CallTask task) {
      this.task = new Task().withCallTask(task);
      return this;
    }

    public ActivityBuilder callTask(CallJava callJava) {
      return callTask(new CallTaskJava(callJava));
    }

    public ActivityBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ActivityBuilder entryCondition(Predicate<WorkflowModel> entryCondition) {
      this.entryCondition = entryCondition;
      return this;
    }

    public ActivityBuilder postAction(Consumer<WorkflowModel> postAction) {
      this.postAction = postAction;
      return this;
    }

    public ActivityBuilder isRepeatable(boolean isRepeatable) {
      this.isRepeatable = isRepeatable;
      return this;
    }

    public Activity build() {
      return new Activity(
          Objects.requireNonNull(task, "Task must be provided"),
          name != null ? name : "activity-" + UUID.randomUUID(),
          Objects.requireNonNull(entryCondition, "Entry condition must be provided"),
          postAction,
          isRepeatable);
    }
  }

  // Activity keeps track of its own execution state, so we need to create a new instance when used
  // In other words, an Activity is a prototype
  Activity newInstance() {
    return new Activity(this.task, name, this.entryCondition, this.postAction, this.isRepeatable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(task, name, entryCondition, postAction);
  }
}
