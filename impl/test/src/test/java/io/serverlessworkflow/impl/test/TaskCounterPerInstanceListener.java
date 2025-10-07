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
package io.serverlessworkflow.impl.test;

import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskStartedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TaskCounterPerInstanceListener implements WorkflowExecutionListener {

  public static class TaskCounter {
    private int started;
    private int completed;

    public void incStarted() {
      started++;
    }

    public void incCompleted() {
      completed++;
    }

    public int started() {
      return started;
    }

    public int completed() {
      return completed;
    }
  }

  private Map<String, TaskCounter> taskCounter = new ConcurrentHashMap<>();

  public void onTaskStarted(TaskStartedEvent ev) {
    taskCounter(ev).incStarted();
  }

  private TaskCounter taskCounter(WorkflowEvent ev) {
    return taskCounter.computeIfAbsent(
        ev.workflowContext().instanceData().id(), k -> new TaskCounter());
  }

  public Optional<TaskCounter> taskCounter(String instanceId) {
    return Optional.ofNullable(taskCounter.get(instanceId));
  }

  public void onTaskCompleted(TaskCompletedEvent ev) {
    taskCounter(ev).incCompleted();
  }
}
