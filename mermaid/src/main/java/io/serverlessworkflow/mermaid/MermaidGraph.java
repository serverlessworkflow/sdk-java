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
import io.serverlessworkflow.api.types.FlowDirective;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MermaidGraph {

  MermaidGraph() {}

  private static FlowDirective extractThen(TaskItem task) {
    TaskBase taskBase = toTaskBase(task);
    if (taskBase == null) {
      return null;
    }
    return taskBase.getThen();
  }

  private static TaskBase toTaskBase(TaskItem task) {
    if (task.getTask() == null || task.getTask().get() == null) {
      return null;
    }
    if (task.getTask().get() instanceof CallTask) {
      return (TaskBase) ((CallTask) task.getTask().get()).get();
    }
    return (TaskBase) task.getTask().get();
  }

  Map<String, Node> buildWithTerminals(List<TaskItem> tasks) {
    final Map<String, Node> graph = this.build(tasks);
    final Node startNode = new Node(Ids.newId(), "__start", NodeType.START);
    final Node endNode = new Node(Ids.newId(), "__end", NodeType.STOP);
    for (Node n : graph.values()) {
      if (n.getNext() == null && n.getType() != NodeType.START && n.getType() != NodeType.STOP) {
        n.setNext(endNode);
      }
    }
    graph.put("start", startNode.withNext(graph.get(tasks.get(0).getName())));
    graph.put("end", endNode);
    return graph;
  }

  Map<String, Node> build(List<TaskItem> tasks) {
    Map<String, Node> graph = new LinkedHashMap<>(Math.max(16, tasks.size() * 2));

    for (int i = 0; i < tasks.size(); i++) {
      TaskItem task = tasks.get(i);
      Node u = graph.computeIfAbsent(task.getName(), n -> NodeBuilder.task(task));
      FlowDirective next = extractThen(task);
      if ((next == null || FlowDirectiveEnum.CONTINUE.equals(next.getFlowDirectiveEnum()))
          && (i + 1 < tasks.size())) {
        TaskItem nextTask = tasks.get(i + 1);
        Node v = graph.computeIfAbsent(nextTask.getName(), n -> NodeBuilder.task(nextTask));
        u.setNext(v);
      }
    }

    for (TaskItem cur : tasks) {
      FlowDirective then = extractThen(cur);
      if (then != null && then.getString() != null) {
        Node from = graph.get(cur.getName());
        Node to = graph.get(then.getString());
        if (to == null) {
          throw new IllegalStateException(
              "then -> '"
                  + then.getString()
                  + "' not found in this task list (from '"
                  + cur.getName()
                  + "')");
        }
        from.setNext(to);
      }
    }

    return graph;
  }
}
