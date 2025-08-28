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

import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.EmitTask;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ListenTask;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;

public final class NodeBuilder {

  private NodeBuilder() {}

  public static Node note(String label) {
    Node node = new Node(Ids.newId(), label, NodeType.NOTE);
    node.setRenderedArrow("-.->");
    return node;
  }

  public static Node junction() {
    return new Node(Ids.newId(), "join", NodeType.JUNCTION);
  }

  public static SplitNode split() {
    return new SplitNode(Ids.newId(), "split");
  }

  public static Node rect(String label) {
    return new Node(Ids.newId(), label, NodeType.RECT);
  }

  public static Node tryBlock() {
    return new TryBlockNode();
  }

  public static Node subgraph(String label) {
    return new SubgraphNode(Ids.newId(), label);
  }

  public static Node task(TaskItem task) {
    if (task.getTask().get() instanceof TryTask) {
      return new TryCatchNode(task);
    } else if (task.getTask().get() instanceof DoTask) {
      return new TaskSubgraphNode(task, String.format("do: %s", task.getName()))
          .withBranches(task.getTask().getDoTask().getDo());
    } else if (task.getTask().get() instanceof ForTask) {
      return new ForNode(task);
    } else if (task.getTask().get() instanceof ListenTask) {
      return new ListenNode(task);
    } else if (task.getTask().get() instanceof EmitTask) {
      return new EmitNode(task);
    } else if (task.getTask().get() instanceof ForkTask) {
      return new ForkNode(task);
    }
    // TODO: Switch, Raise, Run, Set, Wait, Call
    return new TaskNode(task.getName(), task);
  }
}
