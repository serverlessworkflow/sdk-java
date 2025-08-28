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
package io.serverlessworkflow.mermaid;

import io.serverlessworkflow.api.types.CallTask;
import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.EmitTask;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.RaiseTask;
import io.serverlessworkflow.api.types.RunTask;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.SwitchTask;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.WaitTask;
import java.util.Map;

public class TaskNode extends Node {

  protected static final Map<Class<?>, NodeType> NODE_TYPE_BY_CLASS =
      Map.ofEntries(
          Map.entry(CallTask.class, NodeType.RECT),
          Map.entry(DoTask.class, NodeType.SUBGRAPH),
          Map.entry(ForkTask.class, NodeType.SUBGRAPH),
          Map.entry(EmitTask.class, NodeType.EMIT),
          Map.entry(ForTask.class, NodeType.SUBGRAPH),
          Map.entry(ListenTask.class, NodeType.EVENT),
          Map.entry(RaiseTask.class, NodeType.RECT),
          Map.entry(RunTask.class, NodeType.RECT),
          Map.entry(SetTask.class, NodeType.RECT),
          Map.entry(SwitchTask.class, NodeType.SUBGRAPH),
          Map.entry(TryTask.class, NodeType.TRY_CATCH),
          Map.entry(WaitTask.class, NodeType.RECT));
  protected final TaskItem task;

  public TaskNode(String label, TaskItem task) {
    super(Ids.newId(), label);
    this.task = task;

    Object concrete = task.getTask().get();
    Class<?> cls = concrete.getClass();

    this.type = NODE_TYPE_BY_CLASS.getOrDefault(cls, NodeType.RECT);
  }

  public TaskNode(String label, TaskItem task, NodeType type) {
    super(Ids.newId(), label);
    this.task = task;
    this.type = type;
  }
}
